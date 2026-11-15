import { describe, expect, it } from 'vitest';
import { buildPageEntries } from '@/lib/pagination';

describe('buildPageEntries', () => {
  it('lists every page when there are seven pages or fewer', () => {
    expect(buildPageEntries(1, 1)).toEqual([1]);
    expect(buildPageEntries(4, 7)).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });

  it('keeps the first page and a window around the current page', () => {
    expect(buildPageEntries(1, 12)).toEqual([1, 2, 3, 4, 5, 'ellipsis', 12]);
    expect(buildPageEntries(6, 12)).toEqual([1, 'ellipsis', 5, 6, 7, 'ellipsis', 12]);
  });

  it('keeps the last page and a window around the current page', () => {
    expect(buildPageEntries(12, 12)).toEqual([1, 'ellipsis', 8, 9, 10, 11, 12]);
  });

  it('clamps an out-of-range current page', () => {
    expect(buildPageEntries(0, 12)).toEqual([1, 2, 3, 4, 5, 'ellipsis', 12]);
    expect(buildPageEntries(99, 12)).toEqual([1, 'ellipsis', 8, 9, 10, 11, 12]);
  });

  it('never repeats a page number', () => {
    const entries = buildPageEntries(6, 12).filter((entry): entry is number => entry !== 'ellipsis');
    expect(new Set(entries).size).toBe(entries.length);
  });
});
