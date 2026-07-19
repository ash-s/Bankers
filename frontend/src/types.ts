export interface Financials {
  currentPrincipal: number;
  principalPaid: number;
  interestGenerated: number;
  interestPaid: number;
  discountGiven: number;
  interestBalance: number;
  advanceInterestCredit: number;
  cycleInterestAmount: number;
  overdueCycles: number;
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

export interface Transaction {
  id: number;
  date: string;
  type: 'principal' | 'interest' | 'discount';
  amount: number;
  note?: string;
  rateAtPayment?: number;
  interestDueAtPayment?: number;
}

export interface RepledgeDetail {
  id: number;
  placeId: number;
  placeName: string;
  repledgerId: number;
  repledgerName: string;
  date: string;
  amount: number;
  rate: number;
  interestType: 'monthly' | 'daily';
  financials: Financials;
  customerFinancials: Financials;
  rateSpread: number;
  transactions: Transaction[];
}

export interface Loan {
  id: number;
  loanId: string;
  material: string;
  item: string;
  grossWeight: number;
  netWeight: number;
  purity: string;
  pledgeAmount: number;
  pledgeDate: string;
  interestType: 'monthly' | 'daily';
  interestRate: number;
  status: 'active' | 'repledged' | 'completed';
  financials: Financials;
  repledge?: RepledgeDetail;
  transactions: Transaction[];
  materialPhotoFileName?: string;
  hasMaterialPhoto?: boolean;
  materialPhotoUrl?: string;
}

export interface Customer {
  id: number;
  code: string;
  name: string;
  phone: string;
  address?: string;
  idProof?: string;
  idProofFileName?: string;
  hasIdProofFile: boolean;
  idProofUrl?: string;
  loans: Loan[];
}

export interface CustomerListItem {
  id: number;
  code: string;
  name: string;
  phone: string;
  loanCount: number;
  activeLoanCount: number;
  hasOverdue: boolean;
  totalPrincipalDue: number;
  totalInterestDue: number;
}

export interface AppNotification {
  id: number;
  loanId: string;
  customerId: number;
  customerName: string;
  customerPhone: string;
  item: string;
  overdueCycles: number;
  interestBalance: number;
  read: boolean;
  createdAt: string;
}

export interface NotificationSettings {
  duePreset: string;
  customDueDays: number;
  pushEnabled: boolean;
  smsEnabled: boolean;
  whatsappEnabled: boolean;
  smsTemplate?: string;
  whatsappTemplate?: string;
}

export interface Dashboard {
  cashInHandEstimated: number;
  inHandBalance: number;
  openingCapital: number;
  netInterestEarned: number;
  overdueCount: number;
  totalActivePrincipal: number;
  totalPendingInterest: number;
  totalRepledgedPrincipal: number;
  totalOwedToBanks: number;
  totalBorrowedOutstanding: number;
  totalOwedToLenders: number;
  goldInVaultWeight: number;
  goldInVaultCount: number;
  totalReceivable: number;
  totalPayable: number;
  netPosition: number;
  profitLoss: number;
  totalInterestPaidToBanks: number;
  attentionList: AppNotification[];
  criticalAlerts: AppNotification[];
}

export interface Place {
  id: number;
  name: string;
  type: string;
  contact?: string;
  address?: string;
  defaultRate: number;
  activeItems: number;
  totalLiability: number;
}

export interface Repledger {
  id: number;
  name: string;
  role: string;
  phone: string;
}

export interface Lender {
  id: number;
  code: string;
  name: string;
  phone: string;
  address?: string;
  defaultRate: number;
}

export interface Borrowing {
  id: number;
  borrowingId: string;
  lenderId: number;
  lenderName: string;
  amount: number;
  date: string;
  interestType: 'monthly' | 'daily';
  interestRate: number;
  notes?: string;
  status: string;
  financials: Financials;
  transactions: Transaction[];
}

export interface RepledgeListItem {
  loanId: string;
  customerId: number;
  customerName: string;
  item: string;
  customerPrincipal: number;
  bankBorrowed: number;
  placeName: string;
}

export interface InHandEntry {
  id: number;
  type: string;
  amount: number;
  date: string;
  note?: string;
}

export interface InHandSummary {
  openingCapital: number;
  totalPut: number;
  totalTake: number;
  balance: number;
  entries: InHandEntry[];
}

export interface Settings {
  shopName: string;
  shopAddress?: string;
  shopPhone?: string;
  openingCapital: number;
  openingCapitalSet: boolean;
  materials: string[];
}
