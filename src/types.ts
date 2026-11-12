export type Confidence = 'FORT' | 'MOYEN' | 'FAIBLE';

export type UserRole = 'ADMIN' | 'ANALYST' | 'READER';

export type DatePrecision = 'JOUR' | 'MOIS' | 'ANNEE';

/** Niveaux renvoyés par CandidateScorer / DateMatcher. */
export type MatchLevel =
  | 'EXACT'
  | 'TRANSLITTERE'
  | 'PHONETIQUE_FINE'
  | 'PHONETIQUE_GROSSIERE'
  | 'FLOU'
  | 'AUCUN'
  | 'INCOMPLET'
  | 'EXACTE'
  | 'JOUR_MOIS_INVERSE'
  | 'TOLEREE'
  | 'PARTIELLE'
  | 'HYPOTHESE'
  | 'INCOMPATIBLE'
  | string;

export interface FieldExplanation {
  field: string;
  level: MatchLevel;
  detail: string;
  /** Score du champ, entre 0 et 1. */
  score: number;
}

export interface PersonResult {
  id: string;
  /** Identifiant source, conservé pour l'affichage. */
  sourceId: string;
  lastName: string;
  firstName: string;
  fullName: string;
  birthDate: string | null;
  birthDateDisplay: string;
  birthPrecision: DatePrecision | null;
  birthPlace: string;
  fatherName: string;
  motherName: string;
  ownerGroup: string;
  lang: string;
  /** Score métier converti en 0..100 pour l'interface. */
  score: number;
  confidence: Confidence;
  fields: FieldExplanation[];
}

export interface SearchParams {
  mode: 'structured' | 'free';
  lastName: string;
  firstName: string;
  birthDate: string;
  birthPlace: string;
  q: string;
}

export interface SearchResponse {
  results: PersonResult[];
  total: number;
  /** Page 1-based côté interface. */
  page: number;
  pageSize: number;
  totalPages: number;
  requestId: string;
  reliable: boolean;
  message: string;
  warnings: string[];
}

export interface AuthUser {
  username: string;
  role: UserRole;
  roles: UserRole[];
  groups: string[];
  /** En-tête HTTP Basic, conservé uniquement en sessionStorage. */
  authHeader: string;
}

export interface IndexStats {
  numDocs: number;
  maxDoc: number;
  deletedDocs: number;
  path: string;
  open: boolean;
}

export interface SyncReport {
  mode: 'FULL' | 'INCREMENTAL' | string;
  startedAt: string;
  finishedAt: string;
  batches: number;
  rowsRead: number;
  indexed: number;
  deleted: number;
  lastId: string;
  warnings: string[];
}

export interface AuditEntry {
  id: number;
  occurredAt: string;
  username: string;
  roles: string[];
  groups: string[];
  action: string;
  queryHash: string;
  queryKind: string;
  resultCount: number;
  topConfidence: string;
  durationMs: number;
  outcome: string;
  details: string;
}

export interface AuditPage {
  content: AuditEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EvalCategoryMetrics {
  caseCount: number;
  recallAt10: number;
  mrr: number;
  ndcgAt10: number;
  precisionAt1: number;
  strongHits: number;
  falseStrong: number;
}

export interface EvalMetrics {
  topK: number;
  caseCount: number;
  recallAt10: number;
  mrr: number;
  ndcgAt10: number;
  precisionAt1: number;
  strongHits: number;
  falseStrong: number;
  byCategory: Record<string, EvalCategoryMetrics>;
}

export interface BaselineResult {
  name: string;
  available: boolean;
  metrics: EvalMetrics | null;
  note: string;
}

export interface DictionarySuggestion {
  canonical: string;
  observed: string;
  occurrences: number;
  suggestedLine: string;
}

export interface EvalReport {
  generatedAt: string;
  seed: number;
  personCount: number;
  caseCount: number;
  topK: number;
  lucene: EvalMetrics;
  baselines: BaselineResult[];
  suggestions: DictionarySuggestion[];
}

export interface ApiError {
  status: number;
  message: string;
  detail?: string;
}

export interface BackendConfig {
  /** URL du backend ; vide = même origine (proxy Vite en développement). */
  baseUrl: string;
}
