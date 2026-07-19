export function cycleInterest(principal: number, rate: number, type: 'monthly' | 'daily'): number {
  if (!principal || !rate) return 0;
  if (type === 'monthly') return round2((principal * rate) / 100);
  return round2((principal * rate * 30) / 100);
}

export function accrueInterest(
  principal: number,
  rate: number,
  type: 'monthly' | 'daily',
  days: number,
): number {
  if (!principal || !rate || days <= 0) return 0;
  // Kept for daily-rate previews. Monthly calculations require pledge-date
  // calendar boundaries and are handled by calcLoanFinancials.
  if (type === 'monthly') return round2((principal * rate * days) / (100 * 30));
  return round2((principal * rate * days) / 100);
}

export function daysSince(dateStr: string, asOf?: string): number {
  if (!dateStr) return 0;
  const start = parseDate(dateStr);
  const now = new Date();
  const end = asOf
    ? parseDate(asOf)
    : new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()));
  return Math.max(0, Math.floor((end.getTime() - start.getTime()) / 86400000));
}

function daysBetween(start: string, end: string): number {
  return Math.max(0, Math.round((parseDate(end).getTime() - parseDate(start).getTime()) / 86400000));
}

function parseDate(s: string): Date {
  const [year, month, day] = s.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

export interface LoanTx {
  date: string;
  type: 'principal' | 'interest' | 'discount';
  amount: number;
}

export interface LoanFinancials {
  currentPrincipal: number;
  principalPaid: number;
  interestBalance: number;
  advanceInterestCredit: number;
  interestGenerated: number;
  interestPaid: number;
  discountGiven: number;
  cycleInterestAmount: number;
  asOfDate: string;
  interestBreakdown: InterestBreakdown[];
}

export interface InterestBreakdown {
  fromDate: string;
  toDate: string;
  principal: number;
  rate: number;
  interestType: 'monthly' | 'daily';
  days: number;
  cycleDays: number;
  interest: number;
}

/** Mirror backend InterestEngine for client-side preview at a given date. */
export function calcLoanFinancials(
  pledgeAmount: number,
  pledgeDate: string,
  rate: number,
  interestType: 'monthly' | 'daily',
  transactions: LoanTx[],
  calcDate: string,
): LoanFinancials {
  const txs = transactions
    .filter((t) => t.date >= pledgeDate && t.date <= calcDate)
    .sort((a, b) => a.date.localeCompare(b.date) || 0);

  const principalPaid = sumType(txs, 'principal');
  const interestPaid = sumType(txs, 'interest');
  const discountGiven = sumType(txs, 'discount');

  type Period = { start: string; end: string | null; principal: number };
  const periods: Period[] = [];
  let balance = pledgeAmount;
  let periodStart = pledgeDate;

  for (const tx of txs) {
    if (tx.type === 'principal') {
      periods.push({ start: periodStart, end: tx.date, principal: balance });
      balance = Math.max(0, balance - tx.amount);
      periodStart = tx.date;
    }
  }
  periods.push({ start: periodStart, end: null, principal: balance });

  let interestGenerated = 0;
  const interestBreakdown: InterestBreakdown[] = [];
  for (const p of periods) {
    const end = p.end ?? calcDate;
    if (p.start >= end || p.principal <= 0) continue;
    interestGenerated += calculatePeriod(
      pledgeDate, p.start, end, p.principal, rate, interestType, interestBreakdown,
    );
  }
  interestGenerated = round2(interestGenerated);

  const currentPrincipal = round2(Math.max(0, pledgeAmount - principalPaid));
  const rawInterestBalance = round2(interestGenerated - interestPaid - discountGiven);
  const interestBalance = Math.max(0, rawInterestBalance);
  const advanceInterestCredit = Math.max(0, -rawInterestBalance);

  return {
    currentPrincipal,
    principalPaid: round2(principalPaid),
    interestBalance,
    advanceInterestCredit,
    interestGenerated,
    interestPaid: round2(interestPaid),
    discountGiven: round2(discountGiven),
    cycleInterestAmount: cycleInterest(currentPrincipal, rate, interestType),
    asOfDate: calcDate,
    interestBreakdown,
  };
}

function sumType(txs: LoanTx[], type: LoanTx['type']): number {
  return txs.filter((t) => t.type === type).reduce((s, t) => s + t.amount, 0);
}

function calculatePeriod(
  anchor: string,
  from: string,
  to: string,
  principal: number,
  rate: number,
  type: 'monthly' | 'daily',
  breakdown: InterestBreakdown[],
): number {
  if (type === 'daily') {
    const days = daysBetween(from, to);
    const interest = (principal * rate * days) / 100;
    breakdown.push({
      fromDate: from, toDate: to, principal, rate, interestType: type,
      days, cycleDays: 1, interest: round2(interest),
    });
    return interest;
  }

  let total = 0;
  let cursor = from;
  while (cursor < to) {
    const cycle = cycleContaining(anchor, cursor);
    const segmentEnd = to < cycle.end ? to : cycle.end;
    const days = daysBetween(cursor, segmentEnd);
    const cycleDays = daysBetween(cycle.start, cycle.end);
    const interest = (principal * rate * days) / (100 * cycleDays);
    total += interest;
    breakdown.push({
      fromDate: cursor, toDate: segmentEnd, principal, rate, interestType: type,
      days, cycleDays, interest: round2(interest),
    });
    cursor = segmentEnd;
  }
  return total;
}

function cycleContaining(anchor: string, date: string): { start: string; end: string } {
  const anchorDate = parseDate(anchor);
  const dateValue = parseDate(date);
  let monthIndex = Math.max(0,
    (dateValue.getUTCFullYear() - anchorDate.getUTCFullYear()) * 12
      + dateValue.getUTCMonth() - anchorDate.getUTCMonth());

  while (addCalendarMonths(anchor, monthIndex) > date) monthIndex -= 1;
  while (addCalendarMonths(anchor, monthIndex + 1) <= date) monthIndex += 1;
  return {
    start: addCalendarMonths(anchor, monthIndex),
    end: addCalendarMonths(anchor, monthIndex + 1),
  };
}

function addCalendarMonths(anchor: string, months: number): string {
  const d = parseDate(anchor);
  const firstOfTarget = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + months, 1));
  const lastDay = new Date(Date.UTC(
    firstOfTarget.getUTCFullYear(), firstOfTarget.getUTCMonth() + 1, 0,
  )).getUTCDate();
  const result = new Date(Date.UTC(
    firstOfTarget.getUTCFullYear(),
    firstOfTarget.getUTCMonth(),
    Math.min(d.getUTCDate(), lastDay),
  ));
  return result.toISOString().slice(0, 10);
}
