import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  ScrollText,
  XCircle,
} from 'lucide-react';
import { useAuth } from '@/lib/auth';
import type { ApiError, AuditPage } from '@/types';
import { formatDateTime, formatDurationMs, shortHash } from '@/lib/display';
import { localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';
import { confidenceShortLabel } from '@/lib/confidence';

const PAGE_SIZE = 20;

export function AuditLogPanel() {
  const { client } = useAuth();
  const { lang, t } = useLanguage();
  const [auditPage, setAuditPage] = useState<AuditPage | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      setAuditPage(await client.getAuditLog(page - 1, PAGE_SIZE));
    } catch (caught) {
      setError((caught as ApiError).message ?? t('audit.error'));
    } finally {
      setLoading(false);
    }
  }, [client, page, t]);

  useEffect(() => {
    void load();
  }, [load]);

  const entries = auditPage?.content ?? [];
  const totalPages = Math.max(1, auditPage?.totalPages ?? 1);
  const total = auditPage?.totalElements ?? 0;

  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <ScrollText className="h-5 w-5 text-slate-700" />
          <h2 className="text-sm font-semibold text-slate-900">{t('audit.title')}</h2>
        </div>
        <button
          onClick={load}
          disabled={loading}
          className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          {t('audit.refresh')}
        </button>
      </div>

      <p className="mb-3 text-[11px] leading-relaxed text-slate-400">{t('audit.description')}</p>

      {error && (
        <div className="mb-3 flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-xs text-rose-700">
          <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{localizeBackendText(error, lang)}</span>
        </div>
      )}

      {loading && entries.length === 0 ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
        </div>
      ) : entries.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <AlertCircle className="mb-2 h-6 w-6 text-slate-300" />
          <p className="text-xs text-slate-400">{t('audit.empty')}</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-start text-xs">
            <thead>
              <tr className="border-b border-slate-100 text-[10px] uppercase tracking-wide text-slate-400">
                <th className="py-2 pe-3 font-medium">{t('audit.timestamp')}</th>
                <th className="py-2 pe-3 font-medium">{t('audit.user')}</th>
                <th className="py-2 pe-3 font-medium">{t('audit.action')}</th>
                <th className="py-2 pe-3 font-medium">{t('audit.hash')}</th>
                <th className="py-2 pe-3 text-end font-medium">{t('audit.results')}</th>
                <th className="py-2 pe-3 font-medium">{t('audit.confidence')}</th>
                <th className="py-2 pe-3 text-end font-medium">{t('audit.duration')}</th>
                <th className="py-2 pe-0 font-medium">{t('audit.status')}</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr
                  key={entry.id}
                  className="border-b border-slate-50 align-top transition hover:bg-slate-50/50"
                >
                  <td className="whitespace-nowrap py-2.5 pe-3 tabular-nums text-slate-500">
                    {formatDateTime(entry.occurredAt, lang)}
                  </td>
                  <td className="py-2.5 pe-3">
                    <div className="font-medium text-slate-700">{entry.username || '—'}</div>
                    <div className="mt-0.5 text-[10px] text-slate-400">
                      {entry.roles.length > 0 ? entry.roles.join(', ') : '—'}
                      {entry.groups.length > 0 ? ` · ${entry.groups.join(', ')}` : ''}
                    </div>
                  </td>
                  <td className="py-2.5 pe-3">
                    <span className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-[10px] font-medium text-slate-600">
                      {entry.action || '—'}
                    </span>
                    {entry.queryKind && (
                      <div className="mt-0.5 text-[10px] text-slate-400">{entry.queryKind}</div>
                    )}
                  </td>
                  <td
                    className="py-2.5 pe-3 font-mono text-[10px] text-slate-500"
                    title={entry.queryHash || t('audit.noQuery')}
                  >
                    {shortHash(entry.queryHash, 16)}
                  </td>
                  <td className="py-2.5 pe-3 text-end tabular-nums font-semibold text-slate-700">
                    {entry.resultCount}
                  </td>
                  <td className="py-2.5 pe-3 text-slate-600">
                    {confidenceShortLabel(entry.topConfidence, lang) || '—'}
                  </td>
                  <td className="whitespace-nowrap py-2.5 pe-3 text-end tabular-nums text-slate-500">
                    {formatDurationMs(entry.durationMs, lang)}
                  </td>
                  <td className="py-2.5 pe-0">
                    <span
                      className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                        entry.outcome === 'OK'
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-rose-50 text-rose-700'
                      }`}
                      title={entry.details}
                    >
                      {entry.outcome || '—'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
        <span>{t('audit.entries', { count: total })}</span>
        <div className="flex items-center gap-1">
          <button
            onClick={() => setPage((value) => Math.max(1, value - 1))}
            disabled={page <= 1 || loading}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            aria-label={t('audit.previous')}
          >
            <ChevronLeft className="h-4 w-4 rtl:rotate-180" />
          </button>
          <span className="px-2 font-medium tabular-nums text-slate-700">
            {page} / {totalPages}
          </span>
          <button
            onClick={() => setPage((value) => Math.min(totalPages, value + 1))}
            disabled={page >= totalPages || loading}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            aria-label={t('audit.next')}
          >
            <ChevronRight className="h-4 w-4 rtl:rotate-180" />
          </button>
        </div>
      </div>
    </div>
  );
}
