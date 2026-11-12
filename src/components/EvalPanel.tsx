import { useState } from 'react';
import {
  BarChart3,
  FlaskConical,
  Info,
  Loader2,
  Play,
  TrendingUp,
  XCircle,
} from 'lucide-react';
import { useAuth } from '@/lib/auth';
import type { ApiError, EvalMetrics, EvalReport } from '@/types';
import { formatDateTime, formatPercent } from '@/lib/display';
import { localizeBackendText, type TranslationKey } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';

const CATEGORY_KEYS: Record<string, TranslationKey> = {
  exact: 'eval.categoryExact',
  inversion: 'eval.categoryInversion',
  arabic: 'eval.categoryArabic',
  transliteration: 'eval.categoryTransliteration',
  typo: 'eval.categoryTypo',
  date: 'eval.categoryDate',
  homonym: 'eval.categoryHomonym',
};

function falseStrongRate(metrics: EvalMetrics): number {
  return metrics.strongHits === 0 ? 0 : metrics.falseStrong / metrics.strongHits;
}

export function EvalPanel() {
  const { client } = useAuth();
  const { lang, t } = useLanguage();
  const [seed, setSeed] = useState('42');
  const [count, setCount] = useState('40');
  const [report, setReport] = useState<EvalReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const categoryLabel = (value: string) => {
    const key = CATEGORY_KEYS[value];
    return key ? t(key) : value;
  };

  const run = async () => {
    setError(null);
    const seedNumber = Number(seed);
    const countNumber = Number(count);
    if (!Number.isInteger(seedNumber)) {
      setError(t('eval.errorSeed'));
      return;
    }
    if (!Number.isInteger(countNumber) || countNumber < 12 || countNumber > 120) {
      setError(t('eval.errorCount'));
      return;
    }
    setLoading(true);
    try {
      setReport(await client.runEval(seedNumber, countNumber));
    } catch (caught) {
      setError((caught as ApiError).message ?? t('eval.errorRun'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
      <div className="mb-1 flex items-center gap-2">
        <FlaskConical className="h-5 w-5 text-slate-700" />
        <h2 className="text-sm font-semibold text-slate-900">{t('eval.title')}</h2>
      </div>
      <p className="mb-4 text-[11px] leading-relaxed text-slate-400">{t('eval.description')}</p>

      {error && (
        <div className="mb-4 flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-xs text-rose-700">
          <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{localizeBackendText(error, lang)}</span>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="w-full sm:w-32">
          <label className="mb-1 block text-[11px] font-medium text-slate-500">
            {t('eval.seed')}
          </label>
          <input
            type="number"
            value={seed}
            onChange={(event) => setSeed(event.target.value)}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-200"
          />
        </div>
        <div className="w-full sm:w-36">
          <label className="mb-1 block text-[11px] font-medium text-slate-500">
            {t('eval.count')}
          </label>
          <input
            type="number"
            min={12}
            max={120}
            value={count}
            onChange={(event) => setCount(event.target.value)}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-200"
          />
        </div>
        <button
          onClick={run}
          disabled={loading}
          className="flex items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-2.5 text-xs font-semibold text-white transition hover:bg-slate-800 disabled:opacity-60"
        >
          {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
          {t('eval.run')}
        </button>
      </div>

      {report && (
        <div className="mt-5 space-y-5">
          <div className="rounded-xl bg-slate-50 px-3 py-2.5 text-[11px] leading-relaxed text-slate-500">
            {t('eval.report', {
              date: formatDateTime(report.generatedAt, lang),
              seed: report.seed,
              personCount: report.personCount,
              caseCount: report.caseCount,
              topK: report.topK,
            })}
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
            <Metric label={t('eval.recall')} value={formatPercent(report.lucene.recallAt10, lang)} />
            <Metric label={t('eval.mrr')} value={formatPercent(report.lucene.mrr, lang)} />
            <Metric label={t('eval.ndcg')} value={formatPercent(report.lucene.ndcgAt10, lang)} />
            <Metric
              label={t('eval.precision')}
              value={formatPercent(report.lucene.precisionAt1, lang)}
            />
            <Metric
              label={t('eval.falseStrong')}
              value={formatPercent(falseStrongRate(report.lucene), lang)}
              hint={t('eval.falseStrongHint', {
                falseStrong: report.lucene.falseStrong,
                strongHits: report.lucene.strongHits,
              })}
            />
          </div>

          <section>
            <div className="mb-2 flex items-center gap-2">
              <BarChart3 className="h-4 w-4 text-slate-500" />
              <h3 className="text-xs font-semibold text-slate-800">{t('eval.byCategory')}</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[620px] text-start text-xs">
                <thead>
                  <tr className="border-b border-slate-100 text-[10px] uppercase tracking-wide text-slate-400">
                    <th className="py-2 pe-3 font-medium">{t('eval.category')}</th>
                    <th className="py-2 pe-3 text-end font-medium">{t('eval.cases')}</th>
                    <th className="py-2 pe-3 text-end font-medium">{t('eval.recall')}</th>
                    <th className="py-2 pe-3 text-end font-medium">{t('eval.mrr')}</th>
                    <th className="py-2 pe-3 text-end font-medium">{t('eval.precision')}</th>
                    <th className="py-2 pe-3 text-end font-medium">{t('eval.strong')}</th>
                    <th className="py-2 pe-0 text-end font-medium">{t('eval.falseStrongPlural')}</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(report.lucene.byCategory).map(([category, metrics]) => (
                    <tr key={category} className="border-b border-slate-50">
                      <td className="py-2 pe-3 font-medium text-slate-700">
                        {categoryLabel(category)}
                      </td>
                      <td className="py-2 pe-3 text-end tabular-nums text-slate-500">
                        {metrics.caseCount}
                      </td>
                      <td className="py-2 pe-3 text-end tabular-nums text-slate-700">
                        {formatPercent(metrics.recallAt10, lang)}
                      </td>
                      <td className="py-2 pe-3 text-end tabular-nums text-slate-700">
                        {formatPercent(metrics.mrr, lang)}
                      </td>
                      <td className="py-2 pe-3 text-end tabular-nums text-slate-700">
                        {formatPercent(metrics.precisionAt1, lang)}
                      </td>
                      <td className="py-2 pe-3 text-end tabular-nums text-slate-500">
                        {metrics.strongHits}
                      </td>
                      <td className="py-2 pe-0 text-end tabular-nums text-rose-600">
                        {metrics.falseStrong}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section>
            <div className="mb-2 flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-slate-500" />
              <h3 className="text-xs font-semibold text-slate-800">{t('eval.baselines')}</h3>
            </div>
            <div className="space-y-2">
              {report.baselines.map((baseline) => (
                <div
                  key={baseline.name}
                  className="rounded-xl border border-slate-100 bg-slate-50/60 px-3 py-2.5"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-slate-800">{baseline.name}</span>
                      <span
                        className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                          baseline.available
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {baseline.available ? t('eval.available') : t('eval.unavailable')}
                      </span>
                    </div>
                    {baseline.metrics && (
                      <span className="text-[11px] tabular-nums text-slate-600">
                        {t('eval.recall')} {formatPercent(baseline.metrics.recallAt10, lang)} ·{' '}
                        {t('eval.mrr')} {formatPercent(baseline.metrics.mrr, lang)} ·{' '}
                        {t('eval.precision')} {formatPercent(baseline.metrics.precisionAt1, lang)}
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
                    {localizeBackendText(baseline.note, lang)}
                  </p>
                </div>
              ))}
            </div>
          </section>

          <section>
            <div className="mb-2 flex items-center gap-2">
              <Info className="h-4 w-4 text-slate-500" />
              <h3 className="text-xs font-semibold text-slate-800">{t('eval.suggestions')}</h3>
            </div>
            {report.suggestions.length === 0 ? (
              <p className="text-xs text-slate-400">{t('eval.noSuggestion')}</p>
            ) : (
              <div className="space-y-2">
                {report.suggestions.map((suggestion) => (
                  <div
                    key={`${suggestion.canonical}-${suggestion.observed}`}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-slate-100 px-3 py-2"
                  >
                    <div className="text-xs text-slate-600">
                      <span className="font-semibold text-slate-800">{suggestion.canonical}</span>
                      <span className="mx-1 text-slate-400">←</span>
                      <span>{suggestion.observed}</span>
                      <span className="ms-2 text-[10px] text-slate-400">
                        {t('eval.occurrences', { count: suggestion.occurrences })}
                      </span>
                    </div>
                    <code className="rounded bg-slate-100 px-2 py-1 font-mono text-[10px] text-slate-600">
                      {suggestion.suggestedLine}
                    </code>
                  </div>
                ))}
              </div>
            )}
            <p className="mt-2 text-[10px] leading-relaxed text-slate-400">
              {t('eval.suggestionNote')}
            </p>
          </section>
        </div>
      )}
    </div>
  );
}

function Metric({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2.5" title={hint}>
      <div className="text-[10px] font-medium uppercase tracking-wide text-slate-400">{label}</div>
      <div className="mt-0.5 text-sm font-semibold tabular-nums text-slate-900">{value}</div>
      {hint && <div className="mt-0.5 text-[9px] text-slate-400">{hint}</div>}
    </div>
  );
}
