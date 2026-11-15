import { describe, expect, it } from 'vitest';
import {
  candidateCountLabel,
  localizeBackendText,
  translate,
} from '@/lib/i18n';

describe('translate', () => {
  it('returns the French dictionary value by default', () => {
    expect(translate('fr', 'login.submit')).toBe('Se connecter');
  });

  it('returns Arabic text for Arabic', () => {
    expect(translate('ar', 'login.submit')).toBe('تسجيل الدخول');
  });

  it('interpolates named variables', () => {
    expect(translate('fr', 'result.father', { name: 'Ahmed' })).toContain('Ahmed');
  });

  it('falls back to the key for an unknown value', () => {
    expect(translate('fr', 'unknown.key' as never)).toBe('unknown.key');
  });
});

describe('candidateCountLabel', () => {
  it('handles French singular and plural', () => {
    expect(candidateCountLabel('fr', 1)).toBe('1 candidat');
    expect(candidateCountLabel('fr', 4)).toBe('4 candidats');
  });

  it('handles Arabic plural forms', () => {
    expect(candidateCountLabel('ar', 1)).toBe('مرشح واحد');
    expect(candidateCountLabel('ar', 2)).toBe('مرشحان');
    expect(candidateCountLabel('ar', 5)).toBe('5 مرشحين');
    expect(candidateCountLabel('ar', 12)).toBe('12 مرشحًا');
  });
});

describe('localizeBackendText', () => {
  it('keeps French text unchanged in French', () => {
    const text = 'Identifiants invalides ou session expirée.';
    expect(localizeBackendText(text, 'fr')).toBe(text);
  });

  it('translates known backend messages to Arabic', () => {
    expect(localizeBackendText('Identifiants invalides ou session expirée.', 'ar')).toBe(
      'بيانات اعتماد غير صالحة أو جلسة منتهية.'
    );
  });

  it('translates parameterized backend warnings', () => {
    expect(
      localizeBackendText(
        'recherche limitée aux 500 meilleurs candidats Lucene avant reranking',
        'ar'
      )
    ).toContain('500');
  });
});
