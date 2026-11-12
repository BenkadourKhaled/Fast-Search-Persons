import { useCallback, useEffect, useState } from 'react';
import {
  ArrowDownToLine,
  BarChart3,
  CheckCircle2,
  Database,
  FolderOpen,
  Loader2,
  RefreshCw,
  XCircle,
} from 'lucide-react';
import { useAuth } from '@/lib/auth';
import type { ApiError, IndexStats, SyncReport } from '@/types';
import { durationBetween, formatDateTime, formatDurationMs } from '@/lib/display';
import { localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';

type Busy = 'stats' | 'reindex' | 'sync' | null;

export function AdminPanel() {
  const { client } = useAuth();
  const { lang, t } = useLanguage();
  const [stats, setStats] = useState<IndexStats | null>(null);
  const [report, setReport] = useState<SyncReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<Busy>(null);
  const [since, setSince] = useState('');

  const loadStats = useCallback(async () => {
    setError(null);
    setBusy('stats');
    try {
      setStats(await client.getIndexStats());
    } catch (caught) {
      setError((caught as ApiError).message ?? t('admin.errorStats'));
    } finally {
      setBusy(null);
    }
  }, [client, t]);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  const doReindex = async () => {
    setError(null);
    setReport(null);
    setBusy('reindex');
    try {
      const result = await client.reindex();
      setReport(result);
      setStats(await client.getIndexStats());
    } catch (caught) {
      setError((caught as ApiError).message ?? t('admin.errorReindex'));
    } finally {
      setBusy(null);
    }
  };

  const doSync = async () => {
    setError(null);
    setReport(null);
    if (since) {
      const parsed = new Date(since);
      if (Number.isNaN(parsed.getTime())) {
        setError(t('admin.errorSince'));
        return;
      }
    }
    setBusy('sync');
    try {
      const result = await client.sync({
        full: false,
        since: since ? new Date(since).toISOString() : undefined,
      });
      setReport(result);
      setStats(await client.getIndexStats());
    } catch (caught) {
      setError((caught as ApiError).message ?? t('admin.errorSync'));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
      <div className="mb-1 flex items-center gap-2">
        <Database className="h-5 w-5 text-slate-700" />
        <h2 className="text-sm font-semibold text-slate-900">{t('admin.title')}</h2>
      </div>
      <p className="mb-4 text-[11px] leading-relaxed text-slate-400">
        {t('admin.description')}
      </p>

      {error && (
        <div className="mb-4 flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-xs text-rose-700">
          <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{localizeBackendText(error, lang)}</span>
        </div>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <button
          onClick={loadStats}
          disabled={busy !== null}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
        >
          {busy === 'stats' ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <BarChart3 className="h-4 w-4" />
          )}
          {t('admin.refreshStats')}
        </button>
        <button
          onClick={doReindex}
          disabled={busy !== null}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
        >
          {busy === 'reindex' ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <RefreshCw className="h-4 w-4" />
          )}
          {t('admin.reindex')}
        </button>
        <button
          onClick={doSync}
          disabled={busy !== null}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
        >
          {busy === 'sync' ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <ArrowDownToLine className="h-4 w-4" />
          )}
          {t('admin.sync')}
        </button>
      </div>

      <div className="mt-3">
        <label className="mb-1 block text-[11px] font-medium text-slate-500">
          {t('admin.sinceLabel')}
        </label>
        <input
          type="datetime-local"
          value={since}
          onChange={(event) => setSince(event.target.value)}
          className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-200 sm:max-w-xs"
        />
        <p className="mt-1 text-[10px] text-slate-400">{t('admin.sinceHint')}</p>
      </div>

      {stats && (
        <>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Stat label={t('admin.documents')} value={String(stats.numDocs)} />
            <Stat label={t('admin.maxDoc')} value={String(stats.maxDoc)} />
            <Stat label={t('admin.deleted')} value={String(stats.deletedDocs)} />
            <Stat
              label={t('admin.state')}
              value={stats.open ? t('admin.open') : t('admin.closed')}
            />
          </div>
          <div className="mt-3 flex items-start gap-2 rounded-xl bg-slate-50 px-3 py-2.5 text-xs text-slate-500">
            <FolderOpen className="mt-0.5 h-3.5 w-3.5 shrink-0" />
            <span className="break-all font-mono text-[11px]">{stats.path || '—'}</span>
          </div>
        </>
      )}

      {report && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3">
          <div className="flex items-start gap-2 text-xs text-emerald-800">
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <p className="font-semibold">
                {report.mode === 'FULL' ? t('admin.reindexDone') : t('admin.syncDone')}
              </p>
              <p className="mt-1 leading-relaxed">
                {t('admin.reportSummary', {
                  indexed: report.indexed,
                  deleted: report.deleted,
                  rowsRead: report.rowsRead,
                  batches: report.batches,
                  duration: formatDurationMs(
                    durationBetween(report.startedAt, report.finishedAt),
                    lang
                  ),
                })}
              </p>
              <p className="mt-1 text-[11px] text-emerald-700">
                {t('admin.startedAt', { date: formatDateTime(report.startedAt, lang) })}
                {report.lastId ? t('admin.lastId', { id: report.lastId }) : ''}
              </p>
              {report.warnings.length > 0 && (
                <ul className="mt-2 list-disc space-y-0.5 ps-4 text-[11px] text-amber-700">
                  {report.warnings.map((warning, index) => (
                    <li key={index}>{localizeBackendText(warning, lang)}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2.5">
      <div className="text-[10px] font-medium uppercase tracking-wide text-slate-400">{label}</div>
      <div className="mt-0.5 text-sm font-semibold tabular-nums text-slate-900">{value}</div>
    </div>
  );
}
