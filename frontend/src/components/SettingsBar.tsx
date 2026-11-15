import { useState } from 'react';
import { Settings, LogOut, Server, ChevronDown, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useLanguage } from '@/lib/i18n-context';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

export function SettingsBar() {
  const { user, config, setBaseUrl, logout } = useAuth();
  const { t } = useLanguage();
  const [open, setOpen] = useState(false);
  const [urlDraft, setUrlDraft] = useState(config.baseUrl);

  const roleLabel: Record<string, string> = {
    ADMIN: t('settings.roleAdmin'),
    ANALYST: t('settings.roleAnalyst'),
    READER: t('settings.roleReader'),
  };

  return (
    <div className="flex items-center gap-3">
      {/* Backend badge */}
      <span className="flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-semibold text-emerald-700 ring-1 ring-emerald-200">
        <Server className="h-3 w-3" />
        {t('settings.live')}
      </span>

      {/* User badge */}
      <span className="hidden items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-medium text-slate-600 sm:flex">
        <ShieldCheck className="h-3 w-3" />
        {user?.username} · {roleLabel[user?.role ?? 'READER']}
      </span>

      {/* Settings dropdown */}
      <div className="relative">
        <button
          onClick={() => setOpen((o) => !o)}
          aria-label={t('settings.open')}
          className="flex items-center gap-1 rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
        >
          <Settings className="h-4 w-4" />
          <ChevronDown className="h-3 w-3" />
        </button>

        {open && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
            <div className="absolute end-0 z-20 mt-2 w-64 rounded-xl bg-white p-4 shadow-lg ring-1 ring-slate-200">
              <h3 className="mb-3 text-xs font-semibold text-slate-900">
                {t('settings.configTitle')}
              </h3>

              <div className="mb-3 flex items-center justify-between gap-2">
                <span className="text-[11px] font-medium text-slate-500">{t('language.label')}</span>
                <LanguageSwitcher />
              </div>

              <div className="mb-3">
                <label className="mb-1 block text-[11px] font-medium text-slate-500">
                  {t('settings.backendUrl')}
                </label>
                <div className="flex gap-1.5">
                  <input
                    type="text"
                    value={urlDraft}
                    onChange={(e) => setUrlDraft(e.target.value)}
                    placeholder="http://localhost:8080"
                    className="flex-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-200"
                  />
                  <button
                    onClick={() => {
                      // Vide = même origine : le front appelle /api et Vite
                      // relaie vers le backend configuré côté proxy.
                      setBaseUrl(urlDraft.trim());
                      setOpen(false);
                    }}
                    className="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-slate-800"
                  >
                    OK
                  </button>
                </div>
                <p className="mt-1.5 text-[10px] leading-snug text-slate-400">
                  {t('settings.backendUrlHint')}
                </p>
              </div>

              <button
                onClick={() => {
                  logout();
                  setOpen(false);
                }}
                className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs font-medium text-rose-600 transition hover:bg-rose-50"
              >
                <LogOut className="h-3.5 w-3.5" />
                {t('settings.logout')}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
