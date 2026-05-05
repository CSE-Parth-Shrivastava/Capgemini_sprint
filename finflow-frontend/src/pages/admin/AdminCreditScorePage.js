import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, TrendingUp, RefreshCw, Sparkles, ChevronDown, ChevronUp, Info, Clock } from 'lucide-react';
import { applicationApi, creditScoreApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, Card, Spinner, StatusBadge } from '../../components/common';
import { fmtCurrency, fmtDateTime, getLoanTypeLabel } from '../../utils/helpers';

function SubScoreBar({ label, value }) {
  const color = value >= 80 ? '#10b981' : value >= 60 ? '#3b82f6' : value >= 40 ? '#f59e0b' : '#ef4444';
  return (
    <div style={{ marginBottom: '0.75rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3, fontSize: '0.8125rem' }}>
        <span style={{ color: 'var(--navy-700)', fontWeight: 500 }}>{label}</span>
        <span style={{ fontWeight: 700, color }}>{value}/100</span>
      </div>
      <div style={{ height: 7, borderRadius: 999, background: '#f3f4f6', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${value}%`, background: color, borderRadius: 999 }} />
      </div>
    </div>
  );
}

const GRADE_COLOR = { EXCELLENT: '#10b981', GOOD: '#3b82f6', FAIR: '#f59e0b', POOR: '#ef4444' };

export default function AdminCreditScorePage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [app,      setApp]      = useState(null);
  const [scores,   setScores]   = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [assessing,setAssessing]= useState(false);
  const [error,    setError]    = useState('');
  const [showForm, setShowForm] = useState(false);
  const [expanded, setExpanded] = useState(null);

  const [form, setForm] = useState({
    existingEmis: '', creditHistoryMonths: '', numberOfDependents: '',
    hasExistingLoan: false, documentStatus: 'SOME_PENDING',
  });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [appRes, csRes] = await Promise.all([
        applicationApi.getById(id),
        creditScoreApi.getHistory(id).catch(() => ({ data: [] })),
      ]);
      setApp(appRes.data);
      setScores(csRes.data || []);
    } catch { setError('Failed to load.'); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const handleAssess = async () => {
    setAssessing(true); setError('');
    try {
      const payload = {
        existingEmis: Number(form.existingEmis) || 0,
        creditHistoryMonths: Number(form.creditHistoryMonths) || 0,
        numberOfDependents: Number(form.numberOfDependents) || 0,
        hasExistingLoan: form.hasExistingLoan,
        documentStatus: form.documentStatus,
      };
      await creditScoreApi.assess(id, payload);
      await load();
      setShowForm(false);
    } catch (err) { setError(extractError(err)); }
    finally { setAssessing(false); }
  };

  if (loading) return <PageLoader />;
  if (!app)    return <Alert type="error">Application not found.</Alert>;

  const latest = scores[0];

  return (
    <div style={{ maxWidth: 800 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/admin/applications/${id}`)} style={{ padding: '6px 8px' }}>
          <ChevronLeft size={16} />
        </button>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <h2>Credit Score — Application #{id}</h2>
            <StatusBadge status={app.status} />
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--navy-500)', marginTop: 2 }}>
            {app.fullName} · {getLoanTypeLabel(app.loanType)} · {fmtCurrency(app.loanAmount)}
          </div>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => setShowForm(f => !f)}>
          <Sparkles size={13} /> {scores.length > 0 ? 'Re-assess' : 'Run Assessment'}
        </button>
      </div>

      {error && <Alert type="error" onClose={() => setError('')} style={{ marginBottom: '1rem' }}>{error}</Alert>}

      {showForm && (
        <Card style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
          <div style={{ fontWeight: 600, marginBottom: '1rem' }}>Credit Assessment Inputs</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="label">Existing EMIs (₹/month)</label>
              <input className="input" type="number" min="0" placeholder="0" value={form.existingEmis} onChange={e => setForm(f => ({ ...f, existingEmis: e.target.value }))} />
            </div>
            <div>
              <label className="label">Credit History (months)</label>
              <input className="input" type="number" min="0" placeholder="0" value={form.creditHistoryMonths} onChange={e => setForm(f => ({ ...f, creditHistoryMonths: e.target.value }))} />
            </div>
            <div>
              <label className="label">Dependents</label>
              <input className="input" type="number" min="0" max="10" placeholder="0" value={form.numberOfDependents} onChange={e => setForm(f => ({ ...f, numberOfDependents: e.target.value }))} />
            </div>
            <div>
              <label className="label">Document Status</label>
              <select className="input" value={form.documentStatus} onChange={e => setForm(f => ({ ...f, documentStatus: e.target.value }))}>
                <option value="ALL_VERIFIED">All Verified</option>
                <option value="ALL_UPLOADED">All Uploaded</option>
                <option value="SOME_PENDING">Some Pending</option>
                <option value="NONE_UPLOADED">None Uploaded</option>
              </select>
            </div>
          </div>
          <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input type="checkbox" id="el" checked={form.hasExistingLoan} onChange={e => setForm(f => ({ ...f, hasExistingLoan: e.target.checked }))} />
            <label htmlFor="el" style={{ fontSize: '0.875rem' }}>Has existing active loan(s)</label>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.25rem' }}>
            <button className="btn btn-primary" onClick={handleAssess} disabled={assessing}>
              {assessing ? <><Spinner size="sm" /> Calculating...</> : <><Sparkles size={14} /> Calculate</>}
            </button>
            <button className="btn btn-secondary" onClick={() => setShowForm(false)} disabled={assessing}>Cancel</button>
          </div>
          <div style={{ marginTop: '0.625rem', fontSize: '0.75rem', color: 'var(--navy-400)', display: 'flex', gap: '0.375rem', alignItems: 'flex-start' }}>
            <Info size={12} style={{ flexShrink: 0, marginTop: 1 }} />
            Income, loan amount, tenure and employment type are pulled automatically from the application.
          </div>
        </Card>
      )}

      {scores.length === 0 && !showForm && (
        <Card style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <TrendingUp size={44} style={{ color: 'var(--teal-300)', marginBottom: '0.875rem' }} />
          <h3 style={{ marginBottom: '0.5rem' }}>No Credit Score Yet</h3>
          <p style={{ color: 'var(--navy-500)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>Run the credit assessment to evaluate this applicant's creditworthiness.</p>
          <button className="btn btn-primary" onClick={() => setShowForm(true)}><Sparkles size={15} /> Run Assessment</button>
        </Card>
      )}

      {scores.length > 0 && (
        <>
          {/* Latest */}
          <Card style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
            <div style={{ fontWeight: 600, marginBottom: '1.25rem', fontSize: '1rem' }}>Latest Assessment</div>
            <div style={{ display: 'grid', gridTemplateColumns: '200px 1fr', gap: '2rem', alignItems: 'start' }}>
              {/* Big score */}
              <div style={{ textAlign: 'center', padding: '1rem', background: 'var(--navy-50)', borderRadius: 'var(--radius-lg)' }}>
                <div style={{ fontSize: '3.5rem', fontWeight: 900, color: GRADE_COLOR[latest.grade] || '#6b7280', lineHeight: 1 }}>{latest.score}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--navy-400)', margin: '2px 0 8px' }}>out of 850</div>
                <div style={{ display: 'inline-block', padding: '3px 12px', borderRadius: 999, fontSize: '0.75rem', fontWeight: 700, background: (GRADE_COLOR[latest.grade] || '#6b7280') + '20', color: GRADE_COLOR[latest.grade] || '#6b7280' }}>
                  {latest.grade}
                </div>
                <div style={{ marginTop: '0.75rem', fontSize: '0.75rem', fontWeight: 600, color: 'var(--navy-600)' }}>
                  {(latest.recommendation || '').replace(/_/g, ' ')}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--navy-400)', marginTop: 4 }}>{fmtDateTime(latest.assessedAt)}</div>
              </div>

              {/* Sub-scores */}
              <div>
                <SubScoreBar label="Income Stability"     value={latest.incomeStabilityScore} />
                <SubScoreBar label="Debt-to-Income"       value={latest.debtToIncomeScore} />
                <SubScoreBar label="Employment Profile"    value={latest.employmentScore} />
                <SubScoreBar label="Document Verification" value={latest.documentVerificationScore} />
                <SubScoreBar label="Loan Affordability"   value={latest.loanAffordabilityScore} />
              </div>
            </div>

            {/* Report toggle */}
            {latest.aiAnalysis && (
              <>
                <button style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'none', border: 'none', cursor: 'pointer', marginTop: '1rem', fontWeight: 600, color: 'var(--teal-600)', fontSize: '0.875rem', padding: 0 }}
                  onClick={() => setExpanded(expanded === 'latest' ? null : 'latest')}>
                  {expanded === 'latest' ? <ChevronUp size={14} /> : <ChevronDown size={14} />} Full Assessment Report
                </button>
                {expanded === 'latest' && (
                  <pre style={{ marginTop: '0.875rem', fontSize: '0.8125rem', color: 'var(--navy-600)', whiteSpace: 'pre-wrap', fontFamily: 'inherit', lineHeight: 1.7, background: 'var(--navy-50)', padding: '1rem', borderRadius: 'var(--radius-lg)' }}>
                    {latest.aiAnalysis}
                  </pre>
                )}
              </>
            )}
          </Card>

          {/* History */}
          {scores.length > 1 && (
            <Card style={{ padding: '1.25rem' }}>
              <div style={{ fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Clock size={15} /> Score History ({scores.length} assessments)
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {scores.slice(1).map((cs, i) => (
                  <div key={cs.id} style={{ display: 'flex', alignItems: 'center', gap: '1rem', padding: '0.625rem 0.875rem', background: 'var(--navy-50)', borderRadius: 'var(--radius)', fontSize: '0.875rem' }}>
                    <span style={{ fontWeight: 700, color: GRADE_COLOR[cs.grade] || '#6b7280', minWidth: 40 }}>{cs.score}</span>
                    <span style={{ color: 'var(--navy-500)', fontSize: '0.75rem', padding: '2px 8px', background: (GRADE_COLOR[cs.grade] || '#6b7280') + '15', borderRadius: 999 }}>{cs.grade}</span>
                    <span style={{ color: 'var(--navy-400)', fontSize: '0.75rem' }}>{fmtDateTime(cs.assessedAt)}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
