import { describe, expect, it } from 'vitest';
import {
  durationBetween,
  fieldLabel,
  formatDate,
  formatDurationMs,
  formatPercent,
  isRtl,
  langLabel,
  matchLevelColor,
  matchLevelLabel,
  precisionLabel,
  shortHash,
} from '@/lib/display';

describe('display labels', () => {
  it('translates known field names and preserves unknown ones', () => {
    expect(fieldLabel('nom', 'fr')).toBe('Nom');
    expect(fieldLabel('nom', 'ar')).not.toBe('Nom');
    expect(fieldLabel('champ inconnu', 'fr')).toBe('champ inconnu');
  });

  it('translates match levels', () => {
    expect(matchLevelLabel('EXACT', 'fr')).toBeTruthy();
    expect(matchLevelLabel('PHONETIQUE_FINE', 'ar')).toBeTruthy();
    expect(matchLevelColor('EXACT')).toContain('bg-slate-800');
  });

  it('translates precision and language labels', () => {
    expect(precisionLabel('JOUR', 'fr')).toBeTruthy();
    expect(precisionLabel('MOIS', 'ar')).toBeTruthy();
    expect(langLabel('fr', 'fr')).toBeTruthy();
    expect(langLabel('ar', 'fr')).toBeTruthy();
    expect(langLabel('mixte', 'fr')).toBeTruthy();
  });
});

describe('formatting helpers', () => {
  it('formats ISO dates as DD/MM/YYYY', () => {
    expect(formatDate('1992-04-07', 'fr')).toBe('07/04/1992');
    expect(formatDate(null, 'fr')).toBeTruthy();
  });

  it('formats percentages and durations', () => {
    expect(formatPercent(0.812, 'fr', 1)).toContain('81,2');
    expect(formatPercent(null, 'fr')).toBe('—');
    expect(formatDurationMs(500, 'fr')).toBe('500 ms');
    expect(formatDurationMs(1500, 'fr')).toContain('1,5');
    expect(formatDurationMs(-1, 'fr')).toBe('—');
  });

  it('computes non-negative durations', () => {
    expect(
      durationBetween('2026-01-01T10:00:00Z', '2026-01-01T10:00:01Z')
    ).toBe(1000);
    expect(
      durationBetween('2026-01-01T10:00:01Z', '2026-01-01T10:00:00Z')
    ).toBe(0);
  });
});

describe('RTL and hash helpers', () => {
  it('detects Arabic script', () => {
    expect(isRtl('وليد بن علي')).toBe(true);
    expect(isRtl('Oualid Benali')).toBe(false);
  });

  it('shortens long hashes', () => {
    expect(shortHash('abcdef0123456789', 8)).toBe('abcdef01…');
    expect(shortHash('abc', 8)).toBe('abc');
    expect(shortHash('')).toBe('—');
  });
});
