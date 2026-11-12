import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  LANGUAGE_STORAGE_KEY,
  translate,
  type Direction,
  type Language,
} from '@/lib/i18n';
import { LanguageContext, type LanguageContextValue } from '@/lib/i18n-context';

function detectInitialLanguage(): Language {
  try {
    const stored = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (stored === 'fr' || stored === 'ar') return stored;
  } catch {
    // localStorage peut être indisponible (navigation privée stricte).
  }
  if (typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('ar')) {
    return 'ar';
  }
  return 'fr';
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Language>(detectInitialLanguage);
  const dir: Direction = lang === 'ar' ? 'rtl' : 'ltr';

  const setLang = useCallback((next: Language) => {
    setLangState(next);
    try {
      window.localStorage.setItem(LANGUAGE_STORAGE_KEY, next);
    } catch {
      // La préférence reste active pour la session courante.
    }
  }, []);

  const t = useCallback(
    (key: Parameters<typeof translate>[1], vars?: Record<string, string | number>) =>
      translate(lang, key, vars),
    [lang]
  );

  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = dir;
    document.title = translate(lang, 'meta.title');
  }, [lang, dir]);

  const value = useMemo<LanguageContextValue>(
    () => ({ lang, dir, setLang, t }),
    [lang, dir, setLang, t]
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}
