import { AlertTriangle } from 'lucide-react';
import { useLanguage } from '@/lib/i18n-context';

export function CandidateBanner() {
  const { t } = useLanguage();

  return (
    <div className="flex items-start gap-2.5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
      <p className="text-xs leading-relaxed text-amber-800">
        <strong className="font-semibold">{t('banner.strong')}</strong> {t('banner.text')}
      </p>
    </div>
  );
}
