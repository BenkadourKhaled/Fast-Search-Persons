import { useState } from 'react';
import {
  Calendar,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Languages,
  MapPin,
  Shield,
  User,
  Users,
} from 'lucide-react';
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

interface ResultCardProps {
  result: PersonResult;
  rank: number;
  onOpenDetail?: (person: PersonResult) => void;
}

export function ResultCard({ result, rank, onOpenDetail }: ResultCardProps) {
  const [expanded, setExpanded] = useState(false);
  const { lang, t } = useLanguage();
  const conf = confidenceConfig[result.confidence];
  const displayName =
    result.fullName ||
    [result.lastName, result.firstName].filter(Boolean).join(' ') ||
    t('result.unknownName');
  const rtl = isRtl(displayName);

  return (
    <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70 transition hover:shadow-md">
      <div
        className={`flex cursor-pointer items-start gap-4 px-5 py-4 ${onOpenDetail ? 'hover:bg-slate-50/50' : ''}`}
        onClick={() => onOpenDetail?.(result)}
        role={onOpenDetail ? 'button' : undefined}
        tabIndex={onOpenDetail ? 0 : undefined}
        onKeyDown={(event) => {
          if (onOpenDetail && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            onOpenDetail(result);
          }
        }}
      >
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-sm font-bold text-slate-600">
          {rank}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
            <h3
              className="truncate text-base font-semibold text-slate-900"
              dir={rtl ? 'rtl' : 'auto'}
            >
              {displayName}
            </h3>
            {result.lang && (
              <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-500">
                {langLabel(result.lang, lang)}
              </span>
            )}
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
            <span className="flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" />
              {formatDate(result.birthDate, lang)}
              {result.birthPrecision && result.birthPrecision !== 'JOUR' && (
                <span className="ms-1 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-500">
                  {precisionLabel(result.birthPrecision, lang)}
                </span>
              )}
            </span>
            {result.birthPlace && (
              <span className="flex items-center gap-1">
                <MapPin className="h-3.5 w-3.5" />
                {result.birthPlace}
              </span>
            )}
            {result.fatherName && (
              <span className="flex items-center gap-1">
                <User className="h-3.5 w-3.5" />
                {t('result.father', { name: result.fatherName })}
              </span>
            )}
            {result.motherName && (
              <span className="flex items-center gap-1">
                <Users className="h-3.5 w-3.5" />
                {t('result.mother', { name: result.motherName })}
              </span>
            )}
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-2">
            <span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[10px] text-slate-500">
              {result.sourceId}
            </span>
            {result.ownerGroup && (
              <span className="flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-[10px] text-slate-500">
                <Shield className="h-3 w-3" />
                {t('result.group', { group: result.ownerGroup })}
              </span>
            )}
            <span className="flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-[10px] text-slate-500">
              <Languages className="h-3 w-3" />
              {t('result.fieldsExplained', { count: result.fields.length })}
            </span>
          </div>
        </div>

        <div className="flex shrink-0 flex-col items-end gap-2">
          <div className={`text-2xl font-bold tabular-nums ${scoreColor(result.score)}`}>
            {result.score}
            <span className="ms-0.5 text-xs font-medium text-slate-400">/100</span>
          </div>
          <div
            className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-semibold ${conf.bg} ${conf.text} ${conf.border}`}
            title={conf.label[lang]}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${conf.dot}`} />
            {conf.shortLabel[lang]}
          </div>
          {onOpenDetail && (
            <ChevronRight className="mt-1 h-4 w-4 text-slate-300 rtl:rotate-180" />
          )}
        </div>
      </div>

      <div className="h-1 w-full bg-slate-100">
        <div
          className={`h-full ${scoreBarColor(result.score)} transition-all`}
          style={{ width: `${Math.min(100, Math.max(0, result.score))}%` }}
        />
      </div>

      <button
        onClick={() => setExpanded((value) => !value)}
        className="flex w-full items-center justify-between px-5 py-2.5 text-xs font-medium text-slate-600 transition hover:bg-slate-50"
        aria-expanded={expanded}
      >
        <span>{t('result.explanationToggle', { count: result.fields.length })}</span>
        {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>

      {expanded && (
        <div className="border-t border-slate-100 bg-slate-50/50 px-5 py-4">
          {result.fields.length === 0 ? (
            <p className="text-xs text-slate-500">{t('result.noExplanation')}</p>
          ) : (
            <div className="space-y-3">
              {result.fields.map((field, index) => (
                <div
                  key={`${field.field}-${index}`}
                  className="rounded-xl bg-white p-3 ring-1 ring-slate-100"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
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
                      <p className="mt-0.5 text-xs text-slate-500">
                        {localizeBackendText(field.detail, lang)}
                      </p>
                    </div>
                    <div className="shrink-0 text-end">
                      <div className="text-xs font-bold tabular-nums text-slate-700">
                        {Math.round(field.score * 100)} %
                      </div>
                      <div className="text-[10px] text-slate-400">{t('result.similarity')}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-[11px] leading-relaxed text-amber-700">
            {t('result.candidateNotice')}
          </p>
        </div>
      )}
    </div>
  );
}
