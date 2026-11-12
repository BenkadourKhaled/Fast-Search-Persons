import type { DatePrecision, MatchLevel } from '@/types';
import { translate, type Language, type TranslationKey } from '@/lib/i18n';

const FIELD_LABEL_KEYS: Record<string, TranslationKey> = {
  identifiant: 'display.fieldIdentifiant',
  nom: 'display.fieldNom',
  prénom: 'display.fieldPrenom',
  prenom: 'display.fieldPrenom',
  'nom complet': 'display.fieldNomComplet',
  'nom de famille': 'display.fieldNomFamille',
  'date de naissance': 'display.fieldDateNaissance',
  'lieu de naissance': 'display.fieldLieuNaissance',
};

export function fieldLabel(field: string, lang: Language = 'fr'): string {
  const key = FIELD_LABEL_KEYS[field];
  return key ? translate(lang, key) : field;
}

const LEVEL_KEYS: Record<string, TranslationKey> = {
  EXACT: 'display.levelExact',
  TRANSLITTERE: 'display.levelTranslit',
  PHONETIQUE_FINE: 'display.levelPhonFine',
  PHONETIQUE_GROSSIERE: 'display.levelPhonGrossiere',
  FLOU: 'display.levelFlou',
  AUCUN: 'display.levelAucun',
  INCOMPLET: 'display.levelIncomplet',
  EXACTE: 'display.levelDateExacte',
  JOUR_MOIS_INVERSE: 'display.levelJourMoisInverse',
  TOLEREE: 'display.levelTolee',
  PARTIELLE: 'display.levelPartielle',
  HYPOTHESE: 'display.levelHypothese',
  INCOMPATIBLE: 'display.levelIncompatible',
};

const LEVEL_CLASSES: Record<string, string> = {
  EXACT: 'bg-slate-800 text-white',
  TRANSLITTERE: 'bg-indigo-100 text-indigo-700 border border-indigo-200',
  PHONETIQUE_FINE: 'bg-teal-100 text-teal-700 border border-teal-200',
  PHONETIQUE_GROSSIERE: 'bg-amber-100 text-amber-700 border border-amber-200',
  FLOU: 'bg-violet-100 text-violet-700 border border-violet-200',
  AUCUN: 'bg-slate-100 text-slate-500 border border-slate-200',
  INCOMPLET: 'bg-orange-100 text-orange-700 border border-orange-200',
  EXACTE: 'bg-slate-800 text-white',
  JOUR_MOIS_INVERSE: 'bg-orange-100 text-orange-700 border border-orange-200',
  TOLEREE: 'bg-cyan-100 text-cyan-700 border border-cyan-200',
  PARTIELLE: 'bg-slate-100 text-slate-600 border border-slate-200',
  HYPOTHESE: 'bg-rose-100 text-rose-700 border border-rose-200',
  INCOMPATIBLE: 'bg-rose-100 text-rose-700 border border-rose-200',
};

export function matchLevelLabel(level: MatchLevel, lang: Language = 'fr'): string {
  const key = LEVEL_KEYS[level];
  return key ? translate(lang, key) : level;
}

export function matchLevelColor(level: MatchLevel): string {
  return LEVEL_CLASSES[level] ?? 'bg-slate-100 text-slate-500 border border-slate-200';
}

export function formatDate(value: string | null | undefined, lang: Language = 'fr'): string {
  if (!value) return translate(lang, 'display.dateUnknown');
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (match) {
    return `${match[3]}/${match[2]}/${match[1]}`;
  }
  return value;
}

function localeFor(lang: Language): string {
  return lang === 'ar' ? 'ar-DZ' : 'fr-FR';
}

export function formatDateTime(value: string | null | undefined, lang: Language = 'fr'): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString(localeFor(lang), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export function formatPercent(
  value: number | null | undefined,
  lang: Language = 'fr',
  digits = 1
): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return `${(value * 100).toLocaleString(localeFor(lang), {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })} %`;
}

export function formatDurationMs(value: number, lang: Language = 'fr'): string {
  if (!Number.isFinite(value) || value < 0) return '—';
  if (value < 1000) return `${Math.round(value)} ms`;
  return `${(value / 1000).toLocaleString(localeFor(lang), {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })} s`;
}

export function durationBetween(start: string, end: string): number {
  const started = new Date(start).getTime();
  const finished = new Date(end).getTime();
  if (Number.isNaN(started) || Number.isNaN(finished)) return 0;
  return Math.max(0, finished - started);
}

export function precisionLabel(
  precision: DatePrecision | null,
  lang: Language = 'fr'
): string {
  if (precision === 'ANNEE') return translate(lang, 'display.precisionYear');
  if (precision === 'MOIS') return translate(lang, 'display.precisionMonth');
  if (precision === 'JOUR') return translate(lang, 'display.precisionDay');
  return translate(lang, 'display.precisionUnknown');
}

export function langLabel(value: string, uiLang: Language = 'fr'): string {
  const normalized = value.trim().toLowerCase();
  if (normalized === 'fr') return translate(uiLang, 'display.langFr');
  if (normalized === 'ar') return translate(uiLang, 'display.langAr');
  if (normalized === 'mixte') return translate(uiLang, 'display.langMixed');
  return value || '—';
}

export function isRtl(value: string): boolean {
  return /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]/.test(value);
}

export function shortHash(hash: string, length = 12): string {
  if (!hash) return '—';
  return hash.length <= length ? hash : `${hash.slice(0, length)}…`;
}
