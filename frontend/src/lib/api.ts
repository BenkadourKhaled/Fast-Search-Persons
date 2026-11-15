import type {
  ApiError,
  AuditPage,
  AuthUser,
  BackendConfig,
  Confidence,
  DatePrecision,
  EvalReport,
  FieldExplanation,
  IndexStats,
  PersonResult,
  SearchParams,
  SearchResponse,
  SyncReport,
  UserRole,
} from '@/types';
import { formatDate } from '@/lib/display';

export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
export const PAGE_SIZE_DEFAULT = 20;
export const PAGE_SIZE_MAX = 100;

const EVAL_MIN_COUNT = 12;
const EVAL_MAX_COUNT = 120;

/* ------------------------------------------------------------------ */
/* Contrat brut renvoyé par Spring Boot                               */
/* ------------------------------------------------------------------ */

interface RawFieldExplanation {
  field: string;
  level: string;
  detail: string;
  score: number;
}

interface RawSearchHit {
  id: string;
  lastName?: string | null;
  firstName?: string | null;
  fullName?: string | null;
  birthDate?: string | null;
  birthPrecision?: string | null;
  birthPlace?: string | null;
  fatherName?: string | null;
  motherName?: string | null;
  ownerGroup?: string | null;
  lang?: string | null;
  score: number;
  confidence: string;
  explanations?: RawFieldExplanation[] | null;
}

interface RawSearchResponse {
  hits: RawSearchHit[];
  total: number;
  page: number;
  size: number;
  reliable: boolean;
  message?: string | null;
  warnings?: string[] | null;
}

interface RawAuthUser {
  username: string;
  roles: string[];
  groups: string[];
}

interface RawApiError {
  status?: number;
  error?: string;
  message?: string;
  path?: string;
}

/* ------------------------------------------------------------------ */
/* Utilitaires                                                        */
/* ------------------------------------------------------------------ */

function normalizeBaseUrl(baseUrl: string): string {
  const trimmed = baseUrl.trim().replace(/\/+$/, '');
  return trimmed === '/' ? '' : trimmed;
}

export function apiUrl(baseUrl: string, path: string): string {
  return `${normalizeBaseUrl(baseUrl)}${path}`;
}

/** Base64 UTF-8 sans dépendre de `unescape`, pour un en-tête Basic fiable. */
function base64Utf8(value: string): string {
  const bytes = new TextEncoder().encode(value);
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
}

function basicHeader(username: string, password: string): string {
  return `Basic ${base64Utf8(`${username}:${password}`)}`;
}

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(1, Math.max(0, value));
}

function normalizeConfidence(value: string | null | undefined): Confidence {
  if (value === 'FORT' || value === 'MOYEN' || value === 'FAIBLE') return value;
  return 'FAIBLE';
}

function normalizePrecision(value: string | null | undefined): DatePrecision | null {
  if (value === 'JOUR' || value === 'MOIS' || value === 'ANNEE') return value;
  return null;
}

function normalizeRole(value: string | null | undefined): UserRole {
  if (value === 'ADMIN' || value === 'ANALYST' || value === 'READER') return value;
  return 'READER';
}

function roleFromRoles(roles: string[]): UserRole {
  if (roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('ANALYST')) return 'ANALYST';
  return 'READER';
}

function mapHit(hit: RawSearchHit): PersonResult {
  const lastName = hit.lastName ?? '';
  const firstName = hit.firstName ?? '';
  const fullName =
    hit.fullName?.trim() ||
    [lastName, firstName].filter((part) => part.trim().length > 0).join(' ');
  return {
    id: hit.id,
    sourceId: hit.id,
    lastName,
    firstName,
    fullName,
    birthDate: hit.birthDate ?? null,
    birthDateDisplay: formatDate(hit.birthDate),
    birthPrecision: normalizePrecision(hit.birthPrecision),
    birthPlace: hit.birthPlace ?? '',
    fatherName: hit.fatherName ?? '',
    motherName: hit.motherName ?? '',
    ownerGroup: hit.ownerGroup ?? '',
    lang: hit.lang ?? '',
    score: Math.round(clamp01(hit.score) * 100),
    confidence: normalizeConfidence(hit.confidence),
    fields: (hit.explanations ?? []).map(
      (field): FieldExplanation => ({
        field: field.field,
        level: field.level,
        detail: field.detail,
        score: clamp01(field.score),
      })
    ),
  };
}

function mapSearchResponse(raw: RawSearchResponse, requestedPage: number, requestedSize: number): SearchResponse {
  const results = (raw.hits ?? []).map(mapHit);
  const total = Number.isFinite(raw.total) ? raw.total : results.length;
  const size = Number.isFinite(raw.size) && raw.size > 0 ? raw.size : requestedSize;
  const totalPages = Math.max(1, Math.ceil(total / size));
  // Le backend renvoie une page 0-based et la ramène à la dernière page si
  // nécessaire ; on applique la même borne pour rester robuste aux anciennes
  // versions du serveur.
  const serverPage =
    Number.isFinite(raw.page) && raw.page >= 0 ? Math.trunc(raw.page) + 1 : requestedPage;
  const page = Math.min(Math.max(1, serverPage), totalPages);
  return {
    results,
    total,
    page,
    pageSize: size,
    totalPages,
    requestId: createRequestId(),
    reliable: raw.reliable !== false,
    message: raw.message ?? '',
    warnings: raw.warnings ?? [],
  };
}

function createRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `local-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function networkError(): ApiError {
  return {
    status: 0,
    message: 'Connexion impossible',
    detail:
      "Le backend n'est pas joignable. Vérifiez qu'il écoute sur le port 8080 ou l'URL configurée.",
  };
}

async function readBody(response: Response): Promise<RawApiError | null> {
  try {
    return (await response.json()) as RawApiError;
  } catch {
    return null;
  }
}

function httpError(status: number, body: RawApiError | null): ApiError {
  if (status === 401) {
    return {
      status,
      message: 'Identifiants invalides ou session expirée.',
      detail: 'Le backend exige une authentification HTTP Basic valide.',
    };
  }
  if (status === 403) {
    return {
      status,
      message: 'Accès refusé',
      detail: "Votre rôle ne permet pas cette opération. Un administrateur peut vous accorder les droits nécessaires.",
    };
  }
  const message = body?.message?.trim();
  return {
    status,
    message: message || `Erreur ${status}`,
    detail: body?.path ? `Requête : ${body.path}` : undefined,
  };
}

/* ------------------------------------------------------------------ */
/* Client                                                             */
/* ------------------------------------------------------------------ */

export class FastSearchClient {
  private config: BackendConfig;
  private authHeader: string | null;
  private unauthorizedHandler: (() => void) | null = null;

  constructor(config: BackendConfig, authHeader: string | null = null) {
    this.config = config;
    this.authHeader = authHeader;
  }

  setConfig(config: BackendConfig): void {
    this.config = config;
  }

  setAuthHeader(header: string | null): void {
    this.authHeader = header;
  }

  setUnauthorizedHandler(handler: (() => void) | null): void {
    this.unauthorizedHandler = handler;
  }

  private headers(json: boolean): Record<string, string> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (json) headers['Content-Type'] = 'application/json';
    if (this.authHeader) headers.Authorization = this.authHeader;
    return headers;
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
      response = await fetch(apiUrl(this.config.baseUrl, path), {
        ...init,
        headers: this.headers(init.body !== undefined),
      });
    } catch {
      throw networkError();
    }
    if (!response.ok) {
      const body = await readBody(response);
      if (response.status === 401 && this.unauthorizedHandler) {
        this.unauthorizedHandler();
      }
      throw httpError(response.status, body);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  /* ----------------------------- Auth ----------------------------- */

  async login(username: string, password: string): Promise<AuthUser> {
    const header = basicHeader(username, password);
    let response: Response;
    try {
      response = await fetch(apiUrl(this.config.baseUrl, '/api/v1/auth/me'), {
        headers: { Accept: 'application/json', Authorization: header },
      });
    } catch {
      throw networkError();
    }
    if (!response.ok) {
      const body = await readBody(response);
      throw httpError(response.status, body);
    }
    const raw = (await response.json()) as RawAuthUser;
    const roles = Array.isArray(raw.roles) ? raw.roles.map(String) : [];
    return {
      username: raw.username || username,
      role: roleFromRoles(roles),
      roles: roles.map(normalizeRole),
      groups: Array.isArray(raw.groups) ? raw.groups.map(String) : [],
      authHeader: header,
    };
  }

  /* ---------------------------- Search ---------------------------- */

  async search(
    params: SearchParams,
    page = 1,
    pageSize = PAGE_SIZE_DEFAULT
  ): Promise<SearchResponse> {
    const safeSize = Math.min(PAGE_SIZE_MAX, Math.max(1, Math.trunc(pageSize)));
    const safePage = Math.max(1, Math.trunc(page));

    const query = new URLSearchParams({
      page: String(safePage - 1),
      size: String(safeSize),
      explain: 'true',
    });
    if (params.mode === 'free') {
      query.set('q', params.q.trim());
    } else {
      if (params.lastName.trim()) query.set('lastName', params.lastName.trim());
      if (params.firstName.trim()) query.set('firstName', params.firstName.trim());
      if (params.birthDate.trim()) query.set('birthDate', params.birthDate.trim());
      if (params.birthPlace.trim()) query.set('birthPlace', params.birthPlace.trim());
    }
    const raw = await this.request<RawSearchResponse>(`/api/v1/persons/search?${query.toString()}`);
    return mapSearchResponse(raw, safePage, safeSize);
  }

  /* ---------------------------- Admin ----------------------------- */

  async getIndexStats(): Promise<IndexStats> {
    return this.request<IndexStats>('/api/v1/admin/index/stats');
  }

  async reindex(): Promise<SyncReport> {
    return this.request<SyncReport>('/api/v1/admin/index/reindex', { method: 'POST' });
  }

  async sync(options: { full?: boolean; since?: string; batchSize?: number } = {}): Promise<SyncReport> {
    const query = new URLSearchParams();
    if (options.full) query.set('full', 'true');
    if (options.since) query.set('since', options.since);
    if (options.batchSize) query.set('batchSize', String(options.batchSize));
    const suffix = query.size > 0 ? `?${query.toString()}` : '';
    return this.request<SyncReport>(`/api/v1/admin/index/sync${suffix}`, { method: 'POST' });
  }

  /* ---------------------------- Audit ----------------------------- */

  async getAuditLog(page = 0, size = 20): Promise<AuditPage> {
    const query = new URLSearchParams({
      page: String(Math.max(0, Math.trunc(page))),
      size: String(Math.max(1, Math.trunc(size))),
    });
    return this.request<AuditPage>(`/api/v1/admin/audit?${query.toString()}`);
  }

  /* ----------------------------- Eval ----------------------------- */

  async runEval(seed = 42, count = 40): Promise<EvalReport> {
    const safeSeed = Number.isFinite(seed) ? Math.trunc(seed) : 42;
    const safeCount = Math.min(EVAL_MAX_COUNT, Math.max(EVAL_MIN_COUNT, Math.trunc(count)));
    const query = new URLSearchParams({
      seed: String(safeSeed),
      count: String(safeCount),
    });
    return this.request<EvalReport>(`/api/v1/eval/run?${query.toString()}`, { method: 'POST' });
  }
}

export type { PersonResult };
