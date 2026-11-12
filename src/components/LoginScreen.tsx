import { useState } from 'react';
import { AlertCircle, FileSearch, Loader2, Lock, Server, User } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import type { ApiError } from '@/types';

export function LoginScreen() {
  const { login, config, setBaseUrl } = useAuth();
  const { lang, t } = useLanguage();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [urlDraft, setUrlDraft] = useState(config.baseUrl);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError(t('login.errorRequired'));
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
    } catch (caught) {
      setError((caught as ApiError).message ?? t('login.errorFailed'));
    } finally {
      setLoading(false);
    }
  };

  const endpointLabel = config.baseUrl || t('login.proxyEndpoint');

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="absolute end-4 top-4">
        <LanguageSwitcher showLabels />
      </div>

      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center">
          <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-900 text-white">
            <FileSearch className="h-6 w-6" />
          </div>
          <h1 className="text-lg font-bold tracking-tight text-slate-900">FastSearch Persons</h1>
          <p className="mt-0.5 text-xs text-slate-500">{t('login.subtitle')}</p>
        </div>

        <form onSubmit={submit} className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70">
          {error && (
            <div className="mb-4 flex items-start gap-2 rounded-lg bg-rose-50 px-3 py-2.5 text-xs text-rose-700">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{localizeBackendText(error, lang)}</span>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="mb-1.5 block text-xs font-medium text-slate-600">
                {t('login.username')}
              </label>
              <div className="relative">
                <User className="absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder={t('login.usernamePlaceholder')}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pe-4 ps-10 text-sm text-slate-900 placeholder:text-slate-400 transition focus:border-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-200"
                  autoComplete="username"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-medium text-slate-600">
                {t('login.password')}
              </label>
              <div className="relative">
                <Lock className="absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pe-4 ps-10 text-sm text-slate-900 placeholder:text-slate-400 transition focus:border-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-200"
                  autoComplete="current-password"
                />
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 active:scale-[0.98] disabled:opacity-60"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Lock className="h-4 w-4" />}
            {loading ? t('login.loading') : t('login.submit')}
          </button>

          <p className="mt-3 text-center text-[10px] leading-relaxed text-slate-400">
            {t('login.basicNote')}
          </p>

          <button
            type="button"
            onClick={() => setShowSettings((value) => !value)}
            className="mt-3 w-full text-center text-[11px] text-slate-400 transition hover:text-slate-600"
          >
            {showSettings ? t('login.hideConfig') : t('login.showConfig')}
          </button>

          {showSettings && (
            <div className="mt-3 space-y-3 rounded-xl bg-slate-50 p-3">
              <div>
                <label className="mb-1 block text-[11px] font-medium text-slate-500">
                  {t('login.backendUrl')}
                </label>
                <div className="flex gap-1.5">
                  <input
                    type="text"
                    value={urlDraft}
                    onChange={(event) => setUrlDraft(event.target.value)}
                    placeholder={t('login.emptySameOrigin')}
                    className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-200"
                  />
                  <button
                    type="button"
                    onClick={() => setBaseUrl(urlDraft)}
                    className="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-slate-800"
                  >
                    {t('login.apply')}
                  </button>
                </div>
                <p className="mt-1 text-[10px] leading-relaxed text-slate-400">
                  {t('login.configHint')}
                </p>
              </div>

              <div className="rounded-lg bg-white px-3 py-2 text-[10px] text-slate-500 ring-1 ring-slate-200">
                <span className="flex items-center gap-1.5 font-medium text-slate-600">
                  <Server className="h-3 w-3" />
                  {endpointLabel}
                </span>
                <p className="mt-1 leading-relaxed">{t('login.apiNote')}</p>
              </div>
            </div>
          )}
        </form>

        <p className="mt-4 text-center text-[11px] leading-relaxed text-slate-400">
          {t('login.syntheticFooter')}
        </p>
      </div>
    </div>
  );
}
