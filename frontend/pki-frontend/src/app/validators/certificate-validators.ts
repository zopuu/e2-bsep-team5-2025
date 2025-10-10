import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

function addYearsUTC(d: Date, years: number): Date {
  const r = new Date(d.getTime());
  r.setUTCFullYear(r.getUTCFullYear() + years);
  return r;
}

/** yearsValid => notAfter ≤ issuer.notAfter */
export function notBeyondIssuerValidator(getIssuerNotAfter: () => Date | null): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const years = Number(group.get('yearsValid')?.value);
    const issuerNA = getIssuerNotAfter();
    if (!issuerNA || !years || Number.isNaN(years)) return null;

    const now = new Date();
    const candidate = addYearsUTC(now, years);
    return (candidate > issuerNA)
      ? { beyondIssuer: { issuerNotAfter: issuerNA.toISOString(), candidate: candidate.toISOString() } }
      : null;
  };
}

/** pathLenConstraint ≤ issuer.pathLen-1; issuer.pathLen=0 => error */
export function pathLenWithinIssuerValidator(
  getIssuerPathLen: () => number | null
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const requestedRaw = group.get('pathLenConstraint')?.value;
    const requested = (requestedRaw === null || requestedRaw === undefined || requestedRaw === '') ? null : Number(requestedRaw);
    const issuerPL = getIssuerPathLen();

    // ako korisnik nije uneo pathLen → dozvoljeno (server će ograničiti)
    if (requested === null) {
      if (issuerPL !== null && issuerPL <= 0) {
        // čak i bez unosa, issuer ne može da izdaje CA
        return { issuerPathLenZero: true };
      }
      return null;
    }

    if (Number.isNaN(requested) || requested < 0) {
      return { pathLenInvalid: true };
    }

    if (issuerPL === null) {
      // issuer bez ograničenja → sve >=0 prolazi
      return null;
    }

    if (issuerPL <= 0) {
      return { issuerPathLenZero: true };
    }

    const maxAllowed = issuerPL - 1;
    return (requested > maxAllowed) ? { pathLenTooHigh: { maxAllowed } } : null;
  };
}
