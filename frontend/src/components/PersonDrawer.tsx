import { Calendar, FileText, Globe, Hash, MapPin, Shield, User, Users, X } from 'lucide-react';
import type { PersonResult } from '@/types';
import { confidenceConfig, scoreBarColor, scoreColor } from '@/lib/confidence';
import { localizeBackendText } from '@/lib/i18n';
import { useLanguage } from '@/lib/i18n-context';
import {
  fieldLabel,
  formatDate,
  isRtl,
  langLabel,
  matchLevelColor,
  matchLevelLabel,
  precisionLabel,
} from '@/lib/display';

interface PersonDrawerProps {
  person: PersonResult | null;
  onClose: () => void;
}

export function PersonDrawer({ person, onClose }: PersonDrawerProps) {
  const { lang, t } = useLanguage();
  if (!person) return null;
  const conf = confidenceConfig[person.confidence];
  const displayName =
    person.fullName ||
    [person.lastName, person.firstName].filter(Boolean).join(' ') ||
    t('result.unknownName');
  const birthLabel = person.birthDate
    ? `${formatDate(person.birthDate, lang)}${
        person.birthPrecision && person.birthPrecision !== 'JOUR'
          ? ` (${precisionLabel(person.birthPrecision, lang)})`
          : ''
      }`
    : t('drawer.unknownDate');

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />
      <aside
        className="fixed end-0 top-0 z-50 h-full w-full max-w-md overflow-y-auto bg-white shadow-2xl"
        role="dialog"
        aria-modal="true"
        aria-label={t('drawer.title')}
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white/95 px-5 py-4 backdrop-blur">
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-slate-700" />
            <h2 className="text-sm font-semibold text-slate-900">{t('drawer.title')}</h2>
          </div>
          <button
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label={t('drawer.close')}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="px-5 py-5">
          <div className="mb-5">
            <h3
              className="text-xl font-bold tracking-tight text-slate-900"
              dir={isRtl(displayName) ? 'rtl' : 'auto'}
            >
              {displayName}
            </h3>
            <div className="mt-3 flex items-center gap-3">
              <span className={`text-3xl font-bold tabular-nums ${scoreColor(person.score)}`}>
                {person.score}
                <span className="ms-0.5 text-sm font-medium text-slate-400">/100</span>
              </span>
              <span
                className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold ${conf.bg} ${conf.text} ${conf.border}`}
              >
                <span className={`h-2 w-2 rounded-full ${conf.dot}`} />
                {conf.label[lang]}
              </span>
            </div>
            <div className="mt-2 h-1.5 w-full rounded-full bg-slate-100">
              <div
                className={`h-full rounded-full ${scoreBarColor(person.score)} transition-all`}
                style={{ width: `${Math.min(100, Math.max(0, person.score))}%` }}
              />
            </div>
          </div>

          <div className="mb-5 grid grid-cols-2 gap-3">
            <InfoRow icon={Hash} label={t('drawer.sourceId')} value={person.sourceId} />
            <InfoRow
              icon={Shield}
              label={t('drawer.group')}
              value={person.ownerGroup || '—'}
            />
            <InfoRow
              icon={Globe}
              label={t('drawer.language')}
              value={langLabel(person.lang, lang)}
            />
            <InfoRow icon={Calendar} label={t('drawer.birth')} value={birthLabel} />
            {person.birthPlace && (
              <InfoRow icon={MapPin} label={t('drawer.place')} value={person.birthPlace} />
            )}
            {person.fatherName && (
              <InfoRow icon={User} label={t('drawer.father')} value={person.fatherName} />
            )}
            {person.motherName && (
              <InfoRow icon={Users} label={t('drawer.mother')} value={person.motherName} />
            )}
          </div>

          <div className="mb-5">
            <h4 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
              {t('drawer.explanation')}
            </h4>
            {person.fields.length === 0 ? (
              <p className="rounded-xl bg-slate-50 p-3 text-xs text-slate-500 ring-1 ring-slate-100">
                {t('drawer.noExplanation')}
              </p>
            ) : (
              <div className="space-y-2.5">
                {person.fields.map((field, index) => (
                  <div
                    key={`${field.field}-${index}`}
                    className="rounded-xl bg-slate-50 p-3 ring-1 ring-slate-100"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-xs font-semibold text-slate-700">
                          {fieldLabel(field.field, lang)}
                        </span>
                        <span
                          className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${matchLevelColor(
                            field.level
                          )}`}
                        >
                          {matchLevelLabel(field.level, lang)}
                        </span>
                      </div>
                      <span className="shrink-0 text-xs font-bold tabular-nums text-slate-700">
                        {Math.round(field.score * 100)} %
                      </span>
                    </div>
                    <p className="mt-1 text-[11px] leading-relaxed text-slate-500">
                      {localizeBackendText(field.detail, lang)}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
            <p className="text-xs leading-relaxed text-amber-800">
              <strong className="font-semibold">{t('drawer.candidateStrong')}</strong>{' '}
              {t('drawer.candidateNotice')}
            </p>
          </div>
        </div>
      </aside>
    </>
  );
}

function InfoRow({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Hash;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2.5">
      <div className="flex items-center gap-1.5 text-[10px] font-medium uppercase tracking-wide text-slate-400">
        <Icon className="h-3 w-3" />
        {label}
      </div>
      <div className="mt-0.5 break-words text-sm font-medium text-slate-900">{value}</div>
    </div>
  );
}
