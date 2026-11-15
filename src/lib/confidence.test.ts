import { describe, expect, it } from 'vitest';
import {
  confidenceConfig,
  confidenceShortLabel,
  scoreBarColor,
  scoreColor,
} from '@/lib/confidence';

describe('confidence labels', () => {
  it('exposes French and Arabic labels', () => {
    expect(confidenceConfig.FORT.label.fr).toBe('Confiance forte');
    expect(confidenceConfig.FORT.label.ar).toBe('ثقة قوية');
    expect(confidenceShortLabel('MOYEN', 'fr')).toBe('Moyen');
    expect(confidenceShortLabel('MOYEN', 'ar')).toBe('متوسطة');
  });

  it('preserves unknown confidence values', () => {
    expect(confidenceShortLabel('INCONNU', 'fr')).toBe('INCONNU');
  });
});

describe('score colors', () => {
  it('uses green, amber and red thresholds', () => {
    expect(scoreColor(85)).toContain('emerald');
    expect(scoreColor(60)).toContain('amber');
    expect(scoreColor(20)).toContain('rose');
    expect(scoreBarColor(85)).toContain('emerald');
    expect(scoreBarColor(60)).toContain('amber');
    expect(scoreBarColor(20)).toContain('rose');
  });
});
