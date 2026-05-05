import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, TrendingUp, AlertTriangle, CheckCircle2, XCircle, RefreshCw, Sparkles, ChevronDown, ChevronUp, Info } from 'lucide-react';
import { applicationApi, creditScoreApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, Card, Spinner, StatusBadge } from '../../components/common';
import { fmtCurrency, fmtDateTime, getLoanTypeLabel, getEmpTypeLabel, EMPLOYMENT_TYPES } from '../../utils/helpers';

// ── Score gauge ───────────────────────────────────────────────────────────────
function ScoreGauge({ score }) {
  const pct    = ((score - 300) / 550) * 100;
  const color  = score >= 750 ? "var(--green-600)" : score >= 650 ? "var(--blue-500)" : score >= 550 ? "var(--amber-500)" : "var(--red-500)";
  const radius = 80, stroke = 12;
  const circ   = 2 * Math.PI * radius;
  const half   = circ / 2;   // we draw a semi-circle
  const dash   = (pct / 100) * half;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '1rem 0' }}>
      <svg width={220} height={130} viewBox="0 0 220 130">
        {/* track */}
        <path
          d={`M ${stroke} ${110} A ${radius} ${radius} 0 0 1 ${220 - stroke} ${110}`}
          fill="none" stroke="var(--navy-100)" strokeWidth={stroke} strokeLinecap="round"
        />
        {/* filled */}
        <path
          d={`M ${stroke} ${110} A ${radius} ${radius} 0 0 1 ${220 - stroke} ${110}`}
          fill="none" stroke={color} strokeWidth={stroke} strokeLinecap="round"
          strokeDasharray={`${dash} ${half}`}
          style={{ transition: 'stroke-dasharray 1s ease' }}
        />
        {/* score text */}
        <text x="110" y="95" textAnchor="middle" style={{ fontSize: 36, fontWeight: 800, fill: color, fontFamily: 'inherit' }}>{score}</text>
        <text x="110" y="118" textAnchor="middle" style={{ fontSize: 12, fill: '#9ca3af', fontFamily: 'inherit' }}>out of 850</text>
        <text x="20"  y="128" style={{ fontSize: 10, fill: '#d1d5db', fontFamily: 'inherit' }}>300</text>
        <text x="182" y="128" style={{ fontSize: 10, fill: '#d1d5db', fontFamily: 'inherit' }}>850</text>
      </svg>
    </div>
  );
}

// ── Sub-score bar ─────────────────────────────────────────────────────────────
function SubScoreBar({ label, value, tooltip }) {
  const color = value >= 80 ? "var(--green-600)" : value >= 60 ? "var(--blue-500)" : value >= 40 ? "var(--amber-500)" : "var(--red-500)";
  return (
    <div style={{ marginBottom: '0.875rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: '0.8125rem' }}>
        <span style={{ color: 'var(--navy-700)', fontWeight: 500 }}>{label}</span>
        <span style={{ fontWeight: 700, color }}>{value}/100</span>
      </div>
      <div style={{ height: 8, borderRadius: 999, background: 'var(--navy-100)', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${value}%`, background: color, borderRadius: 999, transition: 'width 0.8s ease' }} />
      </div>
    </div>
  );
}

// ── Grade badge ───────────────────────────────────────────────────────────────
const GRADE_STYLE = {
  EXCELLENT: { bg: '#f0fdf4', color: '#166534', border: '#bbf7d0', icon: <CheckCircle2 size={16} /> },
  GOOD:      { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe', icon: <TrendingUp size={16} /> },
  FAIR:      { bg: '#fffbeb', color: '#92400e', border: '#fde68a', icon: <AlertTriangle size={16} /> },
  POOR:      { bg: '#fef2f2', color: '#991b1b', border: '#fecaca', icon: <XCircle size={16} /> },
};

const REC_LABELS = {
  STRONGLY_RECOMMENDED: 'Strongly Recommended',
  RECOMMENDED:          'Recommended',
  CONDITIONAL:          'Conditional',
  NOT_RECOMMENDED:      'Not Recommended',
};

export default function CreditScorePage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [app,      setApp]      = useState(null);
  const [latest,   setLatest]   = useState(null);
  const [loading,  setLoading]  = useState(true);
  const [assessing,setAssessing]= useState(false);
  const [error,    setError]    = useState('');
  const [showForm, setShowForm] = useState(false);
  const [showAnalysis, setShowAnalysis] = useState(false);
  const [aiInsight, setAiInsight] = useState('');
  const [aiLoading, setAiLoading] = useState(false);

  const [form, setForm] = useState({
    existingEmis: '',
    creditHistoryMonths: '',
    numberOfDependents: '',
    hasExistingLoan: false,
    documentStatus: 'SOME_PENDING',
  });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const appRes = await applicationApi.getById(id);
      setApp(appRes.data);
      try {
        const csRes = await creditScoreApi.getLatest(id);
        setLatest(csRes.data);
      } catch { /* no score yet */ }
    } catch {
      setError('Failed to load application.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const handleAssess = async () => {
    setAssessing(true); setError('');
    try {
      const payload = {
        existingEmis:        Number(form.existingEmis)        || 0,
        creditHistoryMonths: Number(form.creditHistoryMonths) || 0,
        numberOfDependents:  Number(form.numberOfDependents)  || 0,
        hasExistingLoan:     form.hasExistingLoan,
        documentStatus:      form.documentStatus,
      };
      const res = await creditScoreApi.assess(id, payload);
      setLatest(res.data);
      setShowForm(false);
      // Fetch Claude AI insight
      fetchAiInsight(res.data, app);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setAssessing(false);
    }
  };

  const fetchAiInsight = async (cs, application) => {
    setAiLoading(true);
    try {
      const response = await fetch('https://api.anthropic.com/v1/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'claude-sonnet-4-20250514',
          max_tokens: 1000,
          messages: [{
            role: 'user',
            content: `You are a senior loan underwriter at FinFlow. Provide a concise, personalised credit insight for this applicant. Be direct, empathetic, and actionable.

Application Details:
- Loan Type: ${application?.loanType}
- Loan Amount: ₹${application?.loanAmount?.toLocaleString('en-IN')}
- Tenure: ${application?.tenureMonths} months
- Employment: ${application?.employmentType}
- Monthly Income: ₹${application?.monthlyIncome?.toLocaleString('en-IN')}

Credit Assessment Result:
- Score: ${cs.score}/850 (${cs.grade})
- Recommendation: ${cs.recommendation}
- Income Stability: ${cs.incomeStabilityScore}/100
- Debt-to-Income: ${cs.debtToIncomeScore}/100
- Employment: ${cs.employmentScore}/100
- Document Verification: ${cs.documentVerificationScore}/100
- Loan Affordability: ${cs.loanAffordabilityScore}/100
- Existing EMIs: ₹${cs.existingEmis?.toLocaleString('en-IN') || 0}/month
- Credit History: ${cs.creditHistoryMonths || 0} months

Write 3 short paragraphs:
1. What the score means for this applicant (personalised to their specific numbers)
2. The 2 biggest strengths and weaknesses in their profile
3. Concrete, specific steps they can take to improve their score

Keep it under 250 words. Be conversational, not robotic.`
          }]
        })
      });
      const data = await response.json();
      const text = data.content?.find(b => b.type === 'text')?.text || '';
      setAiInsight(text);
    } catch {
      setAiInsight('');
    } finally {
      setAiLoading(false);
    }
  };

  if (loading) return <PageLoader />;
  if (!app)    return <Alert type="error">Application not found.</Alert>;

  const gradeStyle = latest ? (GRADE_STYLE[latest.grade] || GRADE_STYLE.FAIR) : null;

  return (
    <div style={{ maxWidth: 720 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/applications/${id}`)} style={{ padding: '6px 8px' }}>
          <ChevronLeft size={16} />
        </button>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <h2>Credit Score</h2>
            <StatusBadge status={app.status} />
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--navy-500)', marginTop: 2 }}>
            Application #{id} · {getLoanTypeLabel(app.loanType)} · {fmtCurrency(app.loanAmount)}
          </div>
        </div>
        {latest && (
          <button className="btn btn-secondary btn-sm" onClick={() => setShowForm(f => !f)}>
            <RefreshCw size={13} /> Re-assess
          </button>
        )}
      </div>

      {error && <Alert type="error" onClose={() => setError('')} style={{ marginBottom: '1rem' }}>{error}</Alert>}

      {/* No score yet */}
      {!latest && !showForm && (
        <Card style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <TrendingUp size={48} style={{ color: 'var(--teal-300)', marginBottom: '1rem' }} />
          <h3 style={{ marginBottom: '0.5rem' }}>No Credit Assessment Yet</h3>
          <p style={{ color: 'var(--navy-500)', fontSize: '0.9rem', maxWidth: 400, margin: '0 auto 1.5rem' }}>
            Run a credit assessment to get your CIBIL-style score (300–850) based on income,
            debt obligations, employment, and documents.
          </p>
          <button className="btn btn-primary" onClick={() => setShowForm(true)}>
            <Sparkles size={16} /> Run Credit Assessment
          </button>
        </Card>
      )}

      {/* Assessment form */}
      {showForm && (
        <Card style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
          <div style={{ fontWeight: 600, fontSize: '1rem', marginBottom: '1.25rem' }}>
            {latest ? 'Re-assess Credit Score' : 'Credit Assessment Inputs'}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="label">Existing Monthly EMIs (₹)</label>
              <input className="input" type="number" min="0" placeholder="e.g. 15000"
                value={form.existingEmis} onChange={e => setForm(f => ({ ...f, existingEmis: e.target.value }))} />
              <div style={{ fontSize: '0.75rem', color: 'var(--navy-400)', marginTop: 2 }}>All existing loan EMI obligations</div>
            </div>
            <div>
              <label className="label">Credit History (months)</label>
              <input className="input" type="number" min="0" placeholder="e.g. 36"
                value={form.creditHistoryMonths} onChange={e => setForm(f => ({ ...f, creditHistoryMonths: e.target.value }))} />
              <div style={{ fontSize: '0.75rem', color: 'var(--navy-400)', marginTop: 2 }}>0 if no prior credit history</div>
            </div>
            <div>
              <label className="label">Number of Dependents</label>
              <input className="input" type="number" min="0" max="10" placeholder="e.g. 2"
                value={form.numberOfDependents} onChange={e => setForm(f => ({ ...f, numberOfDependents: e.target.value }))} />
            </div>
            <div>
              <label className="label">Document Status</label>
              <select className="input" value={form.documentStatus} onChange={e => setForm(f => ({ ...f, documentStatus: e.target.value }))}>
                <option value="ALL_VERIFIED">All Documents Verified</option>
                <option value="ALL_UPLOADED">All Uploaded, Pending Verification</option>
                <option value="SOME_PENDING">Some Documents Missing</option>
                <option value="NONE_UPLOADED">No Documents Uploaded</option>
              </select>
            </div>
          </div>

          <div style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input type="checkbox" id="existingLoan" checked={form.hasExistingLoan}
              onChange={e => setForm(f => ({ ...f, hasExistingLoan: e.target.checked }))} />
            <label htmlFor="existingLoan" style={{ fontSize: '0.875rem', color: 'var(--navy-700)' }}>
              I have existing active loan(s)
            </label>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.25rem' }}>
            <button className="btn btn-primary" onClick={handleAssess} disabled={assessing}>
              {assessing ? <><Spinner size="sm" /> Calculating...</> : <><Sparkles size={15} /> Calculate Score</>}
            </button>
            <button className="btn btn-secondary" onClick={() => setShowForm(false)} disabled={assessing}>Cancel</button>
          </div>

          <div style={{ marginTop: '0.75rem', fontSize: '0.75rem', color: 'var(--navy-400)', display: 'flex', gap: '0.375rem', alignItems: 'flex-start' }}>
            <Info size={13} style={{ flexShrink: 0, marginTop: 1 }} />
            Loan details (income, amount, tenure, employment) are auto-fetched from your application.
          </div>
        </Card>
      )}

      {/* Score result */}
      {latest && !showForm && (
        <>
          {/* Score card */}
          <Card style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', alignItems: 'center' }}>
              <div>
                <ScoreGauge score={latest.score} />
              </div>
              <div>
                {/* Grade */}
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', padding: '6px 14px', borderRadius: 999, background: gradeStyle.bg, color: gradeStyle.color, border: `1px solid ${gradeStyle.border}`, fontSize: '0.875rem', fontWeight: 700, marginBottom: '0.75rem' }}>
                  {gradeStyle.icon} {latest.grade}
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--navy-800)', marginBottom: '0.25rem' }}>
                  {REC_LABELS[latest.recommendation]}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--navy-500)', marginBottom: '1rem' }}>
                  Assessed: {fmtDateTime(latest.assessedAt)}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                  {[
                    { label: 'Monthly Income', value: fmtCurrency(latest.monthlyIncome) },
                    { label: 'Loan Amount', value: fmtCurrency(latest.loanAmount) },
                    { label: 'Existing EMIs', value: fmtCurrency(latest.existingEmis) },
                    { label: 'Credit History', value: `${latest.creditHistoryMonths || 0} mo` },
                  ].map(({ label, value }) => (
                    <div key={label} style={{ background: 'var(--navy-50)', borderRadius: 'var(--radius)', padding: '0.5rem 0.75rem' }}>
                      <div style={{ fontSize: '0.6875rem', color: 'var(--navy-400)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>{label}</div>
                      <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--navy-800)' }}>{value}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </Card>

          {/* Sub-scores */}
          <Card style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
            <div style={{ fontWeight: 600, marginBottom: '1.25rem' }}>Factor Breakdown</div>
            <SubScoreBar label="Income Stability"      value={latest.incomeStabilityScore} />
            <SubScoreBar label="Debt-to-Income Ratio"  value={latest.debtToIncomeScore} />
            <SubScoreBar label="Employment Profile"     value={latest.employmentScore} />
            <SubScoreBar label="Document Verification"  value={latest.documentVerificationScore} />
            <SubScoreBar label="Loan Affordability"     value={latest.loanAffordabilityScore} />
          </Card>

          {/* AI Insight */}
          <Card style={{ padding: '1.5rem', marginBottom: '1.25rem', border: '1px solid var(--teal-100)', background: 'linear-gradient(135deg, var(--teal-50), #fff)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--teal-700)' }}>
              <Sparkles size={16} /> AI-Powered Insight
            </div>
            {aiLoading && (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', color: 'var(--navy-500)', fontSize: '0.875rem' }}>
                <Spinner size="sm" /> Generating personalised insight…
              </div>
            )}
            {!aiLoading && aiInsight && (
              <div style={{ fontSize: '0.875rem', color: 'var(--navy-700)', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                {aiInsight}
              </div>
            )}
            {!aiLoading && !aiInsight && (
              <button className="btn btn-secondary btn-sm" onClick={() => fetchAiInsight(latest, app)}>
                <Sparkles size={13} /> Generate AI Insight
              </button>
            )}
          </Card>

          {/* Raw analysis */}
          {latest.aiAnalysis && (
            <Card style={{ padding: '1.25rem' }}>
              <button style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600, color: 'var(--navy-700)', fontSize: '0.9375rem', padding: 0 }}
                onClick={() => setShowAnalysis(a => !a)}>
                {showAnalysis ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                Detailed Assessment Report
              </button>
              {showAnalysis && (
                <pre style={{ marginTop: '1rem', fontSize: '0.8125rem', color: 'var(--navy-600)', whiteSpace: 'pre-wrap', fontFamily: 'inherit', lineHeight: 1.7 }}>
                  {latest.aiAnalysis}
                </pre>
              )}
            </Card>
          )}
        </>
      )}

      {/* No score + show assess button */}
      {!latest && !showForm && (
        <div style={{ textAlign: 'center', marginTop: '1rem' }}>
          <button className="btn btn-primary btn-lg" onClick={() => setShowForm(true)}>
            <Sparkles size={16} /> Run Credit Assessment
          </button>
        </div>
      )}
    </div>
  );
}
