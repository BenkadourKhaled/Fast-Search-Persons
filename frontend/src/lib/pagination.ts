/**
 * Construit la liste des pages affichées par le composant Pagination.
 *
 * Les première et dernière pages restent toujours accessibles ; autour de la
 * page courante, une fenêtre de trois pages est affichée. Les écarts sont
 * représentés par `'ellipsis'`.
 */
export type PageEntry = number | 'ellipsis';

export function buildPageEntries(current: number, totalPages: number): PageEntry[] {
  const total = Math.max(1, Math.trunc(totalPages));
  const safeCurrent = Math.min(Math.max(1, Math.trunc(current)), total);

  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }

  let start = Math.max(2, safeCurrent - 1);
  let end = Math.min(total - 1, safeCurrent + 1);

  if (safeCurrent <= 4) {
    start = 2;
    end = 5;
  } else if (safeCurrent >= total - 3) {
    start = total - 4;
    end = total - 1;
  }

  const entries: PageEntry[] = [1];
  if (start > 2) entries.push('ellipsis');
  for (let page = start; page <= end; page += 1) entries.push(page);
  if (end < total - 1) entries.push('ellipsis');
  entries.push(total);
  return entries;
}
