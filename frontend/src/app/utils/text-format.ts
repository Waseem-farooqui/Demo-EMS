/**
 * Title-style presentation for names and similar labels: each word starts with a capital letter,
 * remainder lower case. Hyphenated names keep each segment capitalised (e.g. Mary-Jane Smith).
 */
export function toProperNameCase(value: string | null | undefined): string {
  if (value == null) {
    return '';
  }
  const s = String(value).trim().replace(/\s+/g, ' ');
  if (!s) {
    return '';
  }
  const capWord = (w: string): string =>
    w ? w.charAt(0).toLocaleUpperCase() + w.slice(1).toLocaleLowerCase() : w;
  return s
    .split(' ')
    .map(part => part.split('-').map(capWord).join('-'))
    .join(' ');
}

/** Address-style: each line trimmed and word-cased for tidy display. */
export function toProperAddressBlock(value: string | null | undefined): string {
  if (value == null) {
    return '';
  }
  return String(value)
    .split(/\r\n|\r|\n/)
    .map(line => toProperNameCase(line))
    .join('\n')
    .trim();
}

export function normalizeWorkEmail(value: string | null | undefined): string {
  if (value == null) {
    return '';
  }
  return String(value).trim().toLowerCase();
}

export function normalizeNationalInsuranceNumber(value: string | null | undefined): string {
  if (value == null) {
    return '';
  }
  return String(value).trim().replace(/\s+/g, ' ').toUpperCase();
}
