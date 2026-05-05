// ── Loan type metadata ────────────────────────────────────────────────────────
export const LOAN_TYPES = [
  { value: 'PERSONAL',  label: 'Personal Loan',  rate: '14%',  maxTenure: 60,  minTenure: 12 },
  { value: 'HOME',      label: 'Home Loan',       rate: '9%',   maxTenure: 360, minTenure: 12 },
  { value: 'CAR',       label: 'Car Loan',        rate: '10.5%',maxTenure: 84,  minTenure: 12 },
  { value: 'EDUCATION', label: 'Education Loan',  rate: '11%',  maxTenure: 120, minTenure: 12 },
  { value: 'BUSINESS',  label: 'Business Loan',   rate: '15%',  maxTenure: 84,  minTenure: 12 },
  { value: 'GOLD',      label: 'Gold Loan',       rate: '10%',  maxTenure: 36,  minTenure: 3  },
];

export const EMPLOYMENT_TYPES = [
  { value: 'SALARIED',       label: 'Salaried' },
  { value: 'SELF_EMPLOYED',  label: 'Self Employed' },
  { value: 'BUSINESS_OWNER', label: 'Business Owner' },
  { value: 'FREELANCER',     label: 'Freelancer' },
  { value: 'STUDENT',        label: 'Student' },
  { value: 'RETIRED',        label: 'Retired' },
  { value: 'UNEMPLOYED',     label: 'Unemployed' },
];

export const DOCUMENT_TYPES = [
  { value: 'IDENTITY_PROOF', label: 'Identity Proof',  hint: 'Aadhar Card / Passport / Voter ID / PAN Card' },
  { value: 'INCOME_PROOF',   label: 'Income Proof',    hint: 'Salary Slips (3 months) / ITR / Form 16' },
  { value: 'ADDRESS_PROOF',  label: 'Address Proof',   hint: 'Utility Bill / Rent Agreement / Bank Statement' },
  { value: 'BANK_STATEMENT', label: 'Bank Statement',  hint: 'Last 6 months bank statement' },
];

export const APPLICATION_STATUSES = [
  { value: 'DRAFT',          label: 'Draft',          color: 'draft' },
  { value: 'SUBMITTED',      label: 'Submitted',      color: 'submitted' },
  { value: 'DOCS_PENDING',   label: 'Docs Pending',   color: 'docs_pending' },
  { value: 'DOCS_VERIFIED',  label: 'Docs Verified',  color: 'docs_verified' },
  { value: 'UNDER_REVIEW',   label: 'Under Review',   color: 'under_review' },
  { value: 'APPROVED',       label: 'Approved',       color: 'approved' },
  { value: 'REJECTED',       label: 'Rejected',       color: 'rejected' },
  { value: 'CLOSED',         label: 'Closed',         color: 'closed' },
];

// ── Formatters ────────────────────────────────────────────────────────────────
export const fmtCurrency = (val) => {
  if (val == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);
};

export const fmtDate = (val) => {
  if (!val) return '—';
  return new Date(val).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const fmtDateTime = (val) => {
  if (!val) return '—';
  return new Date(val).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

export const fmtTimeAgo = (val) => {
  if (!val) return '';
  const diff = Date.now() - new Date(val).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1)  return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  if (d < 30) return `${d}d ago`;
  return fmtDate(val);
};

export const getLoanTypeLabel = (v) => LOAN_TYPES.find(t => t.value === v)?.label || v;
export const getEmpTypeLabel  = (v) => EMPLOYMENT_TYPES.find(t => t.value === v)?.label || v;
export const getDocTypeLabel  = (v) => DOCUMENT_TYPES.find(t => t.value === v)?.label || v;

export const getStatusLabel = (v) =>
  APPLICATION_STATUSES.find(s => s.value === v)?.label || v?.replace(/_/g, ' ');

// ── Initials ──────────────────────────────────────────────────────────────────
export const initials = (name) => {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map(n => n[0].toUpperCase()).join('');
};

// ── EMI calculator (mirrors backend) ──────────────────────────────────────────
export const calcEmi = (principal, annualRate, months) => {
  if (!principal || !annualRate || !months) return 0;
  const r = annualRate / 1200;
  if (r === 0) return principal / months;
  const pow = Math.pow(1 + r, months);
  return (principal * r * pow) / (pow - 1);
};
