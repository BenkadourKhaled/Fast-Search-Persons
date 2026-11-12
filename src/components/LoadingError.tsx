import { AlertCircle, Loader2 } from 'lucide-react';
import { localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';

export function LoadingState() {
  const { t } = useLanguage();

  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white px-6 py-16">
      <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
      <p className="mt-3 text-sm text-slate-500">{t('loading')}</p>
    </div>
  );
}

export function ErrorState({ message, detail }: { message: string; detail?: string }) {
  const { lang } = useLanguage();

  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-rose-200 bg-rose-50/50 px-6 py-12 text-center">
      <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-100">
        <AlertCircle className="h-6 w-6 text-rose-500" />
      </div>
      <h3 className="text-sm font-semibold text-rose-800">
        {localizeBackendText(message, lang)}
      </h3>
      {detail && (
        <p className="mt-1 max-w-sm text-xs leading-relaxed text-rose-600">
          {localizeBackendText(detail, lang)}
        </p>
      )}
    </div>
  );
}
