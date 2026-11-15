import type { Confidence } from '@/types';
import type { Language } from '@/lib/i18n';

export const confidenceConfig: Record<
  Confidence,
  {
    label: Record<Language, string>;
    shortLabel: Record<Language, string>;
    bg: string;
    text: string;
    border: string;
    dot: string;
  }
> = {
  FORT: {
    label: { fr: 'Confiance forte', ar: 'ثقة قوية' },
    shortLabel: { fr: 'Fort', ar: 'قوية' },
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-200',
    dot: 'bg-emerald-500',
  },
  MOYEN: {
    label: { fr: 'Confiance moyenne', ar: 'ثقة متوسطة' },
    shortLabel: { fr: 'Moyen', ar: 'متوسطة' },
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-200',
    dot: 'bg-amber-500',
  },
  FAIBLE: {
    label: { fr: 'Confiance faible', ar: 'ثقة ضعيفة' },
    shortLabel: { fr: 'Faible', ar: 'ضعيفة' },
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-200',
    dot: 'bg-rose-500',
  },
};

export function confidenceShortLabel(value: string, lang: Language): string {
  const config = confidenceConfig[value as Confidence];
  return config ? config.shortLabel[lang] : value;
}

export function scoreColor(score: number): string {
  if (score >= 70) return 'text-emerald-600';
  if (score >= 45) return 'text-amber-600';
  return 'text-rose-600';
}

export function scoreBarColor(score: number): string {
  if (score >= 70) return 'bg-emerald-500';
  if (score >= 45) return 'bg-amber-500';
  return 'bg-rose-500';
}
