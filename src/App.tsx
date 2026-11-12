import { useCallback, useMemo, useState } from 'react';
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Database,
  Download,
  FileSearch,
  Filter,
  FlaskConical,
  ScrollText,
  ShieldCheck,
  Sparkles,
  Users,
} from 'lucide-react';
import type {
  ApiError,
  Confidence,
  PersonResult,
  SearchParams,
  SearchResponse,
} from '@/types';
import { useAuth } from '@/lib/auth';
import { PAGE_SIZE_DEFAULT, PAGE_SIZE_OPTIONS } from '@/lib/api';
import { candidateCountLabel, localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';
import { LanguageProvider } from '@/lib/i18n-provider';
import { SearchForm } from '@/components/SearchForm';
import { ResultCard } from '@/components/ResultCard';
import { EmptyState } from '@/components/EmptyState';
import { CandidateBanner } from '@/components/CandidateBanner';
import { Pagination } from '@/components/Pagination';
import { LoadingState, ErrorState } from '@/components/LoadingError';
import { AdminPanel } from '@/components/AdminPanel';
import { AuditLogPanel } from '@/components/AuditLogPanel';
import { EvalPanel } from '@/components/EvalPanel';
import { SettingsBar } from '@/components/SettingsBar';
import { LoginScreen } from '@/components/LoginScreen';
import { PersonDrawer } from '@/components/PersonDrawer';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { AuthProvider } from '@/lib/auth';

const EMPTY: SearchParams = {
  mode: 'structured',
  lastName: '',
  firstName: '',
  birthDate: '',
  birthPlace: '',
  q: '',
};

type LoadState = 'idle' | 'loading' | 'success' | 'error' | 'empty' | 'vague';
type AdminTab = 'index' | 'audit';

function asApiError(value: unknown): ApiError {
  if (value && typeof value === 'object' && 'message' in value) {
    return value as ApiError;
  }
  return {
    status: 0,
    message: 'Erreur inattendue',
    detail: value instanceof Error ? value.message : String(value),
  };
}

function downloadJson(data: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

function MainApp() {
  const { user, client, config } = useAuth();
  const { lang, t } = useLanguage();
  const [params, setParams] = useState<SearchParams>(EMPTY);
  const [state, setState] = useState<LoadState>('idle');
  const [response, setResponse] = useState<SearchResponse | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(PAGE_SIZE_DEFAULT);
  const [showAdmin, setShowAdmin] = useState(false);
  const [adminTab, setAdminTab] = useState<AdminTab>('index');
  const [showEval, setShowEval] = useState(false);
  const [confidenceFilter, setConfidenceFilter] = useState<Confidence | 'ALL'>('ALL');
  const [selectedPerson, setSelectedPerson] = useState<PersonResult | null>(null);

  const isAdmin = user?.role === 'ADMIN';
  const canEvaluate = isAdmin || user?.role === 'ANALYST';

  const confidenceFilters: { value: Confidence | 'ALL'; label: string; dot: string }[] = [
    { value: 'ALL', label: t('app.all'), dot: 'bg-slate-400' },
    { value: 'FORT', label: t('app.fort'), dot: 'bg-emerald-500' },
    { value: 'MOYEN', label: t('app.moyen'), dot: 'bg-amber-500' },
    { value: 'FAIBLE', label: t('app.faible'), dot: 'bg-rose-500' },
  ];

  const runSearch = useCallback(
    async (searchParams: SearchParams, targetPage = 1, targetSize = pageSize) => {
      const vague =
        searchParams.mode === 'free'
          ? searchParams.q.trim().length < 2
          : !searchParams.lastName.trim() &&
            !searchParams.firstName.trim() &&
            !searchParams.birthDate.trim() &&
            !searchParams.birthPlace.trim();
      if (vague) {
        setState('vague');
        setResponse(null);
        setError(null);
        return;
      }

      setState('loading');
      setError(null);
      try {
        const result = await client.search(searchParams, targetPage, targetSize);
        setResponse(result);
        setPage(result.page);
        if (result.pageSize !== targetSize) {
          setPageSize(result.pageSize);
        }
        setState(result.results.length > 0 ? 'success' : 'empty');
      } catch (caught) {
        const apiError = asApiError(caught);
        if (apiError.status === 401) {
          // Le fournisseur d'authentification a déjà fermé la session.
          return;
        }
        setError(apiError);
        setState('error');
      }
    },
    [client, pageSize]
  );

  const handlePageChange = useCallback(
    (newPage: number) => {
      setPage(newPage);
      void runSearch(params, newPage, pageSize);
    },
    [params, pageSize, runSearch]
  );

  const handlePageSizeChange = useCallback(
    (newSize: number) => {
      setPageSize(newSize);
      setPage(1);
      void runSearch(params, 1, newSize);
    },
    [params, runSearch]
  );

  const reset = () => {
    setParams(EMPTY);
    setResponse(null);
    setState('idle');
    setError(null);
    setPage(1);
    setConfidenceFilter('ALL');
  };

  const allResults = useMemo(() => response?.results ?? [], [response]);
  const filteredResults = useMemo(
    () =>
      confidenceFilter === 'ALL'
        ? allResults
        : allResults.filter((result) => result.confidence === confidenceFilter),
    [allResults, confidenceFilter]
  );

  const fort = allResults.filter((result) => result.confidence === 'FORT').length;
  const moyen = allResults.filter((result) => result.confidence === 'MOYEN').length;
  const faible = allResults.filter((result) => result.confidence === 'FAIBLE').length;

  const exportResults = () => {
    if (!response) return;
    const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
    downloadJson(
      {
        exportedAt: new Date().toISOString(),
        query: params,
        page: response.page,
        pageSize: response.pageSize,
        total: response.total,
        reliable: response.reliable,
        message: response.message,
        warnings: response.warnings,
        results: allResults,
      },
      `fastsearch-export-${stamp}.json`
    );
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-900 text-white">
              <FileSearch className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-base font-bold tracking-tight text-slate-900">
                FastSearch Persons
              </h1>
              <p className="text-[11px] text-slate-500">{t('app.subtitle')}</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden items-center gap-4 text-xs text-slate-500 md:flex">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="h-3.5 w-3.5" />
                {t('app.syntheticData')}
              </span>
            </div>
            <LanguageSwitcher />
            <SettingsBar />
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-slate-400">
            <Sparkles className="h-3.5 w-3.5" />
            {t('app.heroEyebrow')}
          </div>
          <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            {t('app.heroTitle1')}
            <br className="hidden sm:block" /> {t('app.heroTitle2')}
          </h2>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-500">
            {t('app.heroSubtitle')}
          </p>
        </div>

        {isAdmin && (
          <div className="mb-4">
            <button
              onClick={() => setShowAdmin((value) => !value)}
              className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
            >
              <span className="flex items-center gap-2">
                <Database className="h-4 w-4" />
                {t('app.adminPanel')}
              </span>
              {showAdmin ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
            {showAdmin && (
              <div className="mt-3">
                <div className="mb-3 flex rounded-lg bg-slate-100 p-0.5">
                  <button
                    onClick={() => setAdminTab('index')}
                    className={`flex flex-1 items-center justify-center gap-1.5 rounded-md py-2 text-xs font-medium transition ${
                      adminTab === 'index' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'
                    }`}
                  >
                    <Database className="h-3.5 w-3.5" />
                    {t('app.indexTab')}
                  </button>
                  <button
                    onClick={() => setAdminTab('audit')}
                    className={`flex flex-1 items-center justify-center gap-1.5 rounded-md py-2 text-xs font-medium transition ${
                      adminTab === 'audit' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'
                    }`}
                  >
                    <ScrollText className="h-3.5 w-3.5" />
                    {t('app.auditTab')}
                  </button>
                </div>
                {adminTab === 'index' ? <AdminPanel /> : <AuditLogPanel />}
              </div>
            )}
          </div>
        )}

        {canEvaluate && (
          <div className="mb-4">
            <button
              onClick={() => setShowEval((value) => !value)}
              className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
            >
              <span className="flex items-center gap-2">
                <FlaskConical className="h-4 w-4" />
                {t('app.evalPanel')}
              </span>
              {showEval ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
            {showEval && (
              <div className="mt-3">
                <EvalPanel />
              </div>
            )}
          </div>
        )}

        <div className="mb-6">
          <SearchForm
            params={params}
            onChange={setParams}
            onSubmit={() => {
              setPage(1);
              void runSearch(params, 1, pageSize);
            }}
            onReset={reset}
          />
        </div>

        <div className="mb-6">
          <CandidateBanner />
        </div>

        {state === 'loading' && <LoadingState />}

        {state === 'error' && error && <ErrorState message={error.message} detail={error.detail} />}

        {state === 'vague' && <EmptyState searched={true} tooVague={true} />}

        {state === 'empty' && <EmptyState searched={true} tooVague={false} />}

        {state === 'idle' && <EmptyState searched={false} tooVague={false} />}

        {state === 'success' && response && (
          <div>
            <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <Users className="h-4 w-4" />
                <span className="font-semibold text-slate-900">
                  {candidateCountLabel(lang, response.total)}
                </span>
                {confidenceFilter !== 'ALL' && (
                  <span className="text-xs text-slate-400">
                    {t('app.filteredSuffix', { count: filteredResults.length })}
                  </span>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <div
                  className="flex items-center gap-3 text-xs"
                  title={t('app.confidenceDistribution')}
                >
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-emerald-500" />
                    {fort}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-amber-500" />
                    {moyen}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="h-2 w-2 rounded-full bg-rose-500" />
                    {faible}
                  </span>
                </div>

                <button
                  onClick={exportResults}
                  className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-50"
                >
                  <Download className="h-3.5 w-3.5" />
                  {t('app.exportJson')}
                </button>
              </div>
            </div>

            {(!response.reliable || response.warnings.length > 0) && (
              <div className="mb-4 flex items-start gap-2.5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
                <div className="text-xs leading-relaxed text-amber-800">
                  <p className="font-semibold">
                    {response.reliable ? t('app.engineWarnings') : t('app.resultPrudent')}
                  </p>
                  {response.message && (
                    <p className="mt-0.5">{localizeBackendText(response.message, lang)}</p>
                  )}
                  {response.warnings.length > 0 && (
                    <ul className="mt-1 list-disc space-y-0.5 ps-4">
                      {response.warnings.map((warning, index) => (
                        <li key={index}>{localizeBackendText(warning, lang)}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            )}

            <div className="mb-4 flex flex-wrap items-center gap-2">
              <Filter className="h-3.5 w-3.5 text-slate-400" />
              <span className="text-[11px] text-slate-400">{t('app.localFilter')}</span>
              <div className="flex flex-wrap gap-1.5">
                {confidenceFilters.map((filter) => (
                  <button
                    key={filter.value}
                    onClick={() => setConfidenceFilter(filter.value)}
                    className={`flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium transition ${
                      confidenceFilter === filter.value
                        ? 'bg-slate-900 text-white'
                        : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <span className={`h-2 w-2 rounded-full ${filter.dot}`} />
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>

            {filteredResults.length > 0 ? (
              <div className="space-y-3">
                {filteredResults.map((result, index) => (
                  <ResultCard
                    key={result.id}
                    result={result}
                    rank={(response.page - 1) * response.pageSize + index + 1}
                    onOpenDetail={setSelectedPerson}
                  />
                ))}
              </div>
            ) : (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-white/50 px-6 py-10 text-center">
                <p className="text-sm font-semibold text-slate-700">
                  {t('app.noCandidateAtLevel')}
                </p>
                <p className="mt-1 text-xs text-slate-400">{t('app.noCandidateAtLevelHint')}</p>
              </div>
            )}

            {response.totalPages > 1 && (
              <div className="mt-6">
                <Pagination
                  page={page}
                  totalPages={response.totalPages}
                  pageSize={response.pageSize}
                  total={response.total}
                  onPageChange={handlePageChange}
                  onPageSizeChange={handlePageSizeChange}
                  pageSizeOptions={PAGE_SIZE_OPTIONS}
                />
              </div>
            )}
          </div>
        )}

        <footer className="mt-12 border-t border-slate-200 pt-6">
          <p className="text-[11px] leading-relaxed text-slate-400">
            {t('app.footerText', {
              connection: config.baseUrl
                ? t('app.footerBackend', { url: config.baseUrl })
                : t('app.footerProxy'),
            })}
          </p>
        </footer>
      </main>

      <PersonDrawer person={selectedPerson} onClose={() => setSelectedPerson(null)} />
    </div>
  );
}

function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) return <LoginScreen />;
  return <MainApp />;
}

export default function AppWithAuth() {
  return (
    <LanguageProvider>
      <AuthProvider>
        <App />
      </AuthProvider>
    </LanguageProvider>
  );
}
