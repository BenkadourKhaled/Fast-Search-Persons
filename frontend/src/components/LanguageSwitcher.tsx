import { Languages } from 'lucide-react';
import { useLanguage } from '@/lib/i18n-context';

interface LanguageSwitcherProps {
  showLabels?: boolean;
}

export function LanguageSwitcher({ showLabels = false }: LanguageSwitcherProps) {
  const { lang, setLang, t } = useLanguage();

  const buttonClass = (active: boolean) =>
    `rounded-md font-semibold transition ${
      showLabels ? 'px-2.5 py-1 text-[11px]' : 'px-2 py-1 text-[10px]'
    } ${
      active
        ? 'bg-white text-slate-900 shadow-sm'
        : 'text-slate-500 hover:text-slate-700'
    }`;

  return (
    <div
      className="flex items-center gap-1.5"
      role="group"
      aria-label={t('language.switcherLabel')}
      title={t('language.switcherLabel')}
      dir="ltr"
    >
      <Languages className="h-3.5 w-3.5 text-slate-400" aria-hidden="true" />
      <div className="flex rounded-lg bg-slate-100 p-0.5">
        <button
          type="button"
          onClick={() => setLang('fr')}
          aria-pressed={lang === 'fr'}
          aria-label="Français"
          title={t('language.fr')}
          className={buttonClass(lang === 'fr')}
        >
          {showLabels ? 'Français' : 'FR'}
        </button>
        <button
          type="button"
          onClick={() => setLang('ar')}
          aria-pressed={lang === 'ar'}
          aria-label="العربية"
          title={t('language.ar')}
          className={buttonClass(lang === 'ar')}
        >
          {showLabels ? 'العربية' : 'AR'}
        </button>
      </div>
    </div>
  );
}
