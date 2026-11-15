# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 0.1.x | Yes |
| < 0.1 | No |

## Reporting a vulnerability

Please **do not open a public issue** for a security vulnerability.

Use GitHub's private vulnerability reporting feature (Security → Advisories →
Report a vulnerability) for this repository. If private reporting is not
available, contact the maintainers privately and include:

- the affected version or commit;
- the impacted endpoint or component;
- reproduction steps or a proof of concept;
- the potential impact;
- any suggested mitigation.

We will acknowledge the report as soon as possible and coordinate a fix and
disclosure timeline with you.

## Security baseline

- All bundled people, names, dates, places and identifiers are **synthetic**.
- The demo accounts in `src/main/resources/application.yml` are public and must
  be replaced before any real deployment.
- HTTP Basic must be served behind TLS.
- The audit log stores a SHA-256 fingerprint of the query, not the raw query.
- Results are candidates, never automatic identifications.
