package com.fastsearch.persons.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fastsearch.persons.search.Confidence;
import com.fastsearch.persons.search.SearchRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Baseline OpenSearch optionnelle. Aucun appel réseau n'est fait si aucun
 * point de terminaison n'est configuré ; le rapport indique alors la baseline
 * comme indisponible, sans inventer de métriques.
 */
public final class OpenSearchBaseline {

    private static final Pattern INDEX_NAME = Pattern.compile("[a-z0-9][a-z0-9_\\-]{0,127}");

    private final URI endpoint;
    private final String index;
    private final Duration timeout;
    private final ObjectMapper mapper;

    public OpenSearchBaseline(URI endpoint, String index, Duration timeout, ObjectMapper mapper) {
        this.endpoint = endpoint;
        this.index = index == null ? "" : index.trim();
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    public boolean configured() {
        return endpoint != null && INDEX_NAME.matcher(index).matches();
    }

    public BaselineResult evaluate(SyntheticDataset dataset, int topK) {
        if (!configured()) {
            return BaselineResult.unavailable("OpenSearch",
                    "aucun point de terminaison ou nom d'index valide configuré");
        }
        int k = Math.max(1, topK);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
            List<EvalOutcome> outcomes = new ArrayList<>(dataset.cases().size());
            for (EvalCase evalCase : dataset.cases()) {
                List<String> ids = search(client, evalCase.request(), k);
                List<Confidence> confidences = new ArrayList<>();
                for (int i = 0; i < ids.size(); i++) {
                    confidences.add(Confidence.MOYEN);
                }
                outcomes.add(new EvalOutcome(evalCase.id(), evalCase.category(),
                        evalCase.expectedIds(), ids, confidences));
            }
            return BaselineResult.available("OpenSearch",
                    MetricsCalculator.compute(outcomes, k),
                    "multi_match sur les champs nominaux, sans phonétique locale");
        } catch (Exception e) {
            return BaselineResult.unavailable("OpenSearch",
                    "appel impossible (" + e.getClass().getSimpleName() + ")");
        }
    }

    private List<String> search(HttpClient client, SearchRequest request, int size) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", size);
        ObjectNode query = body.putObject("query").putObject("multi_match");
        query.put("query", queryText(request));
        ArrayNode fields = query.putArray("fields");
        fields.add("lastName").add("firstName").add("fullName");
        String url = endpoint.toString().replaceAll("/+$", "") + "/" + index + "/_search";
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenSearch HTTP " + response.statusCode());
        }
        JsonNode root = mapper.readTree(response.body());
        List<String> ids = new ArrayList<>();
        for (JsonNode hit : root.path("hits").path("hits")) {
            String id = hit.path("_source").path("id").asText("");
            if (id.isBlank()) {
                id = hit.path("_id").asText("");
            }
            if (!id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static String queryText(SearchRequest request) {
        if (!request.q().isBlank()) {
            return request.q();
        }
        return (request.lastName() + " " + request.firstName()).trim();
    }
}
