import { Search, SlidersHorizontal, X, Info } from 'lucide-react';
import type { SearchParams } from '@/types';
import { useLanguage } from '@/lib/i18n-context';

interface SearchFormProps {
  params: SearchParams;
  onChange: (p: SearchParams) => void;
  onSubmit: () => void;
  onReset: () => void;
}

export function SearchForm({ params, onChange, onSubmit, onReset }: SearchFormProps) {
  const { t } = useLanguage();
  const isFree = params.mode === 'free';

  return (
    <div className="rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70">
      <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
        <div className="flex items-center gap-2">
          <Search className="h-5 w-5 text-slate-700" />
          <h2 className="text-base font-semibold text-slate-900">{t('search.title')}</h2>
        </div>
        <div className="flex rounded-lg bg-slate-100 p-0.5">
          <button
            onClick={() => onChange({ ...params, mode: 'structured' })}
            className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition ${
              !isFree ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            {t('search.structured')}
          </button>
          <button
            onClick={() => onChange({ ...params, mode: 'free' })}
            className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition ${
              isFree ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <Search className="h-3.5 w-3.5" />
            {t('search.free')}
          </button>
        </div>
      </div>

      <div className="px-6 py-5">
        {isFree ? (
          <div>
            <label className="mb-1.5 block text-xs font-medium text-slate-600">
              {t('search.freeLabel')}
            </label>
            <input
              type="text"
              value={params.q}
              onChange={(e) => onChange({ ...params, q: e.target.value })}
              onKeyDown={(e) => e.key === 'Enter' && onSubmit()}
              placeholder={t('search.freePlaceholder')}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 transition focus:border-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-200"
            />
            <p className="mt-2 flex items-start gap-1.5 text-xs text-slate-400">
              <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" />
              {t('search.freeHint')}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field
              label={t('search.lastName')}
              value={params.lastName}
              placeholder={t('search.lastNamePlaceholder')}
              onChange={(v) => onChange({ ...params, lastName: v })}
              onSubmit={onSubmit}
            />
            <Field
              label={t('search.firstName')}
              value={params.firstName}
              placeholder={t('search.firstNamePlaceholder')}
              onChange={(v) => onChange({ ...params, firstName: v })}
              onSubmit={onSubmit}
            />
            <Field
              label={t('search.birthDate')}
              value={params.birthDate}
              placeholder={t('search.birthDatePlaceholder')}
              onChange={(v) => onChange({ ...params, birthDate: v })}
              onSubmit={onSubmit}
            />
            <Field
              label={t('search.birthPlace')}
              value={params.birthPlace}
              placeholder={t('search.birthPlacePlaceholder')}
              onChange={(v) => onChange({ ...params, birthPlace: v })}
              onSubmit={onSubmit}
            />
          </div>
        )}

        <div className="mt-5 flex items-center justify-between">
          <button
            onClick={onReset}
            className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="h-3.5 w-3.5" />
            {t('search.reset')}
          </button>
          <button
            onClick={onSubmit}
            className="flex items-center gap-2 rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 active:scale-[0.98]"
          >
            <Search className="h-4 w-4" />
            {t('search.submit')}
          </button>
        </div>
      </div>
    </div>
  );
}

function Field({
  label,
  value,
  placeholder,
  onChange,
  onSubmit,
}: {
  label: string;
  value: string;
  placeholder: string;
  onChange: (v: string) => void;
  onSubmit: () => void;
}) {
  return (
    <div>
      <label className="mb-1.5 block text-xs font-medium text-slate-600">{label}</label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && onSubmit()}
        placeholder={placeholder}
        className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 transition focus:border-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-slate-200"
      />
    </div>
  );
}
