import { SearchX, ShieldAlert } from 'lucide-react';
import { useLanguage } from '@/lib/i18n-context';

export function EmptyState({ searched, tooVague }: { searched: boolean; tooVague: boolean }) {
  const { t } = useLanguage();

  if (!searched) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/50 px-6 py-16 text-center">
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100">
          <SearchX className="h-7 w-7 text-slate-400" />
        </div>
        <h3 className="text-sm font-semibold text-slate-700">{t('empty.idleTitle')}</h3>
        <p className="mt-1 max-w-sm text-xs leading-relaxed text-slate-400">
          {t('empty.idleText')}
        </p>
      </div>
    );
  }

  if (tooVague) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-amber-200 bg-amber-50/50 px-6 py-16 text-center">
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100">
          <ShieldAlert className="h-7 w-7 text-amber-500" />
        </div>
        <h3 className="text-sm font-semibold text-amber-800">{t('empty.vagueTitle')}</h3>
        <p className="mt-1 max-w-sm text-xs leading-relaxed text-amber-600">
          {t('empty.vagueText')}
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/50 px-6 py-16 text-center">
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100">
        <SearchX className="h-7 w-7 text-slate-400" />
      </div>
      <h3 className="text-sm font-semibold text-slate-700">{t('empty.noResultTitle')}</h3>
      <p className="mt-1 max-w-sm text-xs leading-relaxed text-slate-400">
        {t('empty.noResultText')}
      </p>
    </div>
  );
}
