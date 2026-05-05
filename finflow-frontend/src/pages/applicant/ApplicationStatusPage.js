import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ChevronLeft, RefreshCw, CheckCircle2, XCircle,
  Clock, FileText, MessageSquare, User, Calendar
} from 'lucide-react';
import { applicationApi, adminApi } from '../../api/services';
import { PageLoader, Alert, Card, StatusBadge } from '../../components/common';
import { fmtDateTime, fmtCurrency, getLoanTypeLabel } from '../../utils/helpers';

// Which statuses count as a final admin decision
const DECIDED = ['APPROVED', 'REJECTED'];
const IN_PROGRESS = ['SUBMITTED', 'DOCS_PENDING', 'DOCS_VERIFIED', 'UNDER_REVIEW'];

export default function ApplicationStatusPage() {
  const { id }     = useParams();
  const navigate   = useNavigate();

  const [app,        setApp]        = useState(null);
  const [decision,   setDecision]   = useState(null);
  const [loading,    setLoading]    = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error,      setError]      = useState('');

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    try {
      const appRes = await applicationApi.getById(id);
      setApp(appRes.data);

      // Try to fetch the admin decision for this application
      try {
        const decRes = await adminApi.getDecisionByApp(id);
        setDecision(decRes.data);
      } catch {
        setDecision(null); // No decision yet — that's fine
      }
    } catch {
      setError('Failed to load application status.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <PageLoader />;
  if (!app)    return <Alert type="error">Application not found.</Alert>;

  const status   = app.status;
  const decided  = DECIDED.includes(status);
  const approved = status === 'APPROVED';
  const rejected = status === 'REJECTED';
  const inProg   = IN_PROGRESS.includes(status);
  const isDraft  = status === 'DRAFT';

  // Remarks: prefer decision.remarks, fall back to app.remarks
  const remarks = decision?.remarks || app.remarks || '';

  return (
    <div style={{ maxWidth: 640 }}>

      {/* ── Back + title ── */}
      <div style={{ display:'flex', alignItems:'center', gap:'0.875rem', marginBottom:'1.75rem', flexWrap:'wrap' }}>
        <button className="btn btn-ghost btn-sm" style={{ padding:'6px 8px' }}
          onClick={() => navigate(`/applications/${id}`)}>
          <ChevronLeft size={16} />
        </button>
        <div style={{ flex:1 }}>
          <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', flexWrap:'wrap' }}>
            <h2 style={{ fontSize:'1.25rem' }}>Application #{id} — Status</h2>
            <StatusBadge status={status} />
          </div>
          <div style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:3 }}>
            {getLoanTypeLabel(app.loanType)} · {fmtCurrency(app.loanAmount)}
          </div>
        </div>
        <button className="btn btn-secondary btn-sm" onClick={() => load(true)} disabled={refreshing}>
          <RefreshCw size={13} style={{ animation: refreshing ? 'spin 0.8s linear infinite' : 'none' }} />
          Refresh
        </button>
      </div>

      {error && <Alert type="error" style={{ marginBottom:'1rem' }}>{error}</Alert>}

      {/* ── APPROVED ── */}
      {approved && (
        <Card style={{ overflow:'hidden', marginBottom:'1.25rem' }}>
          {/* Coloured top strip */}
          <div style={{ height:6, background:'linear-gradient(90deg,var(--green-500),var(--teal-400))' }} />
          <div style={{ padding:'2rem 2rem 1.75rem' }}>
            <div style={{ display:'flex', alignItems:'flex-start', gap:'1rem' }}>
              <div style={{ width:52, height:52, borderRadius:'50%', background:'var(--green-50)', border:'2px solid var(--green-100)', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                <CheckCircle2 size={26} style={{ color:'var(--green-600)' }} />
              </div>
              <div>
                <h3 style={{ color:'var(--green-700)', marginBottom:'0.375rem', fontSize:'1.25rem' }}>
                  Loan Approved 🎉
                </h3>
                <p style={{ color:'var(--navy-600)', fontSize:'0.9375rem', lineHeight:1.7 }}>
                  Congratulations! Your loan application has been reviewed and approved by our team.
                </p>
              </div>
            </div>

            {/* Approved loan terms */}
            {decision && (
              <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(140px,1fr))', gap:'0.875rem', marginTop:'1.5rem' }}>
                {[
                  { label:'Approved Amount', value: decision.approvedAmount ? fmtCurrency(decision.approvedAmount) : fmtCurrency(app.loanAmount) },
                  { label:'Interest Rate',   value: decision.interestRate ? `${decision.interestRate}% p.a.` : '—' },
                  { label:'Tenure',          value: decision.tenureMonths ? `${decision.tenureMonths} months` : `${app.tenureMonths} months` },
                  { label:'Decision Date',   value: decision.decidedAt ? fmtDateTime(decision.decidedAt) : '—' },
                ].map(({ label, value }) => (
                  <div key={label} style={{ padding:'0.875rem 1rem', background:'var(--green-50)', borderRadius:'var(--radius-lg)', border:'1px solid var(--green-100)' }}>
                    <div style={{ fontSize:'0.7rem', fontWeight:700, color:'var(--green-600)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.25rem' }}>{label}</div>
                    <div style={{ fontWeight:700, color:'var(--navy-900)', fontSize:'0.9375rem' }}>{value}</div>
                  </div>
                ))}
              </div>
            )}

            {/* Admin remarks */}
            {remarks && (
              <div style={{ marginTop:'1.25rem', padding:'1rem 1.125rem', background:'var(--navy-50)', borderRadius:'var(--radius-lg)', border:'1px solid var(--navy-100)', display:'flex', gap:'0.75rem', alignItems:'flex-start' }}>
                <MessageSquare size={16} style={{ color:'var(--teal-600)', flexShrink:0, marginTop:2 }} />
                <div>
                  <div style={{ fontSize:'0.75rem', fontWeight:700, color:'var(--navy-500)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.375rem' }}>Admin Remarks</div>
                  <div style={{ fontSize:'0.9375rem', color:'var(--navy-700)', lineHeight:1.65 }}>"{remarks}"</div>
                </div>
              </div>
            )}

            {/* Decided by */}
            {decision?.adminId && (
              <div style={{ marginTop:'1rem', display:'flex', alignItems:'center', gap:'0.5rem', fontSize:'0.8125rem', color:'var(--navy-400)' }}>
                <User size={13} />
                <span>Decision by Admin #{decision.adminId}</span>
                {decision.decidedAt && (
                  <><Calendar size={13} style={{ marginLeft:'0.5rem' }} /><span>{fmtDateTime(decision.decidedAt)}</span></>
                )}
              </div>
            )}
          </div>
        </Card>
      )}

      {/* ── REJECTED ── */}
      {rejected && (
        <Card style={{ overflow:'hidden', marginBottom:'1.25rem' }}>
          <div style={{ height:6, background:'linear-gradient(90deg,var(--red-500),#f87171)' }} />
          <div style={{ padding:'2rem 2rem 1.75rem' }}>
            <div style={{ display:'flex', alignItems:'flex-start', gap:'1rem' }}>
              <div style={{ width:52, height:52, borderRadius:'50%', background:'var(--red-50)', border:'2px solid var(--red-100)', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                <XCircle size={26} style={{ color:'var(--red-600)' }} />
              </div>
              <div>
                <h3 style={{ color:'var(--red-700)', marginBottom:'0.375rem', fontSize:'1.25rem' }}>
                  Application Rejected
                </h3>
                <p style={{ color:'var(--navy-600)', fontSize:'0.9375rem', lineHeight:1.7 }}>
                  Unfortunately your loan application could not be approved at this time.
                  Please review the reason below and consider reapplying after addressing the concerns.
                </p>
              </div>
            </div>

            {/* Rejection reason */}
            {remarks ? (
              <div style={{ marginTop:'1.5rem', padding:'1.25rem 1.25rem', background:'var(--red-50)', borderRadius:'var(--radius-lg)', border:'1.5px solid var(--red-100)', display:'flex', gap:'0.875rem', alignItems:'flex-start' }}>
                <MessageSquare size={17} style={{ color:'var(--red-500)', flexShrink:0, marginTop:2 }} />
                <div>
                  <div style={{ fontSize:'0.75rem', fontWeight:700, color:'var(--red-600)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.5rem' }}>
                    Reason / Admin Remarks
                  </div>
                  <div style={{ fontSize:'0.9375rem', color:'var(--red-800)', lineHeight:1.7 }}>
                    "{remarks}"
                  </div>
                </div>
              </div>
            ) : (
              <div style={{ marginTop:'1.5rem', padding:'1rem 1.125rem', background:'var(--navy-50)', borderRadius:'var(--radius-lg)', border:'1px solid var(--navy-100)', fontSize:'0.875rem', color:'var(--navy-500)', fontStyle:'italic' }}>
                No specific remarks were provided for this decision. Please contact support for further information.
              </div>
            )}

            {/* Decided by */}
            {decision?.adminId && (
              <div style={{ marginTop:'1rem', display:'flex', alignItems:'center', gap:'0.5rem', fontSize:'0.8125rem', color:'var(--navy-400)' }}>
                <User size={13} />
                <span>Decision by Admin #{decision.adminId}</span>
                {decision.decidedAt && (
                  <><Calendar size={13} style={{ marginLeft:'0.5rem' }} /><span>{fmtDateTime(decision.decidedAt)}</span></>
                )}
              </div>
            )}

            <button className="btn btn-primary btn-sm" style={{ marginTop:'1.5rem' }}
              onClick={() => navigate('/applications/new')}>
              Start a New Application
            </button>
          </div>
        </Card>
      )}

      {/* ── IN PROGRESS ── */}
      {inProg && (
        <Card style={{ overflow:'hidden', marginBottom:'1.25rem' }}>
          <div style={{ height:6, background:'linear-gradient(90deg,var(--teal-500),var(--blue-500))' }} />
          <div style={{ padding:'2rem 2rem 1.75rem' }}>
            <div style={{ display:'flex', alignItems:'flex-start', gap:'1rem' }}>
              <div style={{ width:52, height:52, borderRadius:'50%', background:'var(--teal-50)', border:'2px solid var(--teal-100)', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                <Clock size={26} style={{ color:'var(--teal-600)' }} />
              </div>
              <div>
                <h3 style={{ color:'var(--teal-700)', marginBottom:'0.375rem', fontSize:'1.25rem' }}>
                  Application Under Process
                </h3>
                <p style={{ color:'var(--navy-600)', fontSize:'0.9375rem', lineHeight:1.7 }}>
                  {status === 'SUBMITTED'     && 'Your application has been submitted. Please upload all required documents to proceed.'}
                  {status === 'DOCS_PENDING'  && 'Our team is waiting for your supporting documents. Please upload them as soon as possible.'}
                  {status === 'DOCS_VERIFIED' && 'Your documents have been verified. Your application is queued for admin review.'}
                  {status === 'UNDER_REVIEW'  && 'A loan officer is actively reviewing your application. This typically takes 2–5 business days.'}
                </p>
              </div>
            </div>

            <div style={{ marginTop:'1.5rem', padding:'1rem 1.25rem', background:'var(--teal-50)', borderRadius:'var(--radius-lg)', border:'1px solid var(--teal-100)', fontSize:'0.875rem', color:'var(--teal-700)', lineHeight:1.6 }}>
              <strong>What happens next?</strong>
              {status === 'SUBMITTED'     && ' Upload your identity, income, and address proof documents. We will verify them and proceed.'}
              {status === 'DOCS_PENDING'  && ' Upload your pending documents so we can verify them and move to the review stage.'}
              {status === 'DOCS_VERIFIED' && ' An admin will review your complete application. You will be notified once a decision is made.'}
              {status === 'UNDER_REVIEW'  && ' The review is in progress. You will receive a notification when a decision has been taken.'}
            </div>

            {(status === 'SUBMITTED' || status === 'DOCS_PENDING') && (
              <button className="btn btn-primary btn-sm" style={{ marginTop:'1.25rem' }}
                onClick={() => navigate('/documents')}>
                <FileText size={14} /> Upload Documents
              </button>
            )}
          </div>
        </Card>
      )}

      {/* ── DRAFT ── */}
      {isDraft && (
        <Card style={{ padding:'2rem', marginBottom:'1.25rem', textAlign:'center' }}>
          <FileText size={40} style={{ color:'var(--navy-300)', marginBottom:'0.875rem' }} />
          <h3 style={{ color:'var(--navy-700)', marginBottom:'0.5rem' }}>Application is in Draft</h3>
          <p style={{ color:'var(--navy-500)', fontSize:'0.9375rem', lineHeight:1.7, marginBottom:'1.25rem' }}>
            This application has not been submitted yet. Submit it to start the review process.
          </p>
          <button className="btn btn-primary" onClick={() => navigate(`/applications/${id}`)}>
            Go to Application
          </button>
        </Card>
      )}

      {/* ── Application summary ── */}
      <Card style={{ padding:'1.25rem 1.5rem' }}>
        <div style={{ fontWeight:700, color:'var(--navy-800)', marginBottom:'1rem', fontSize:'0.9375rem' }}>Application Summary</div>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(140px,1fr))', gap:'0.75rem' }}>
          {[
            { label:'Application ID', value:`#${app.id}` },
            { label:'Loan Type',      value: getLoanTypeLabel(app.loanType) },
            { label:'Loan Amount',    value: fmtCurrency(app.loanAmount) },
            { label:'Tenure',         value:`${app.tenureMonths} months` },
            { label:'Submitted',      value: app.submittedAt ? fmtDateTime(app.submittedAt) : '—' },
            { label:'Current Status', value:<StatusBadge status={status} /> },
          ].map(({ label, value }) => (
            <div key={label} style={{ padding:'0.75rem', background:'var(--navy-50)', borderRadius:'var(--radius)', border:'1px solid var(--navy-100)' }}>
              <div style={{ fontSize:'0.6875rem', fontWeight:700, color:'var(--navy-400)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.25rem' }}>{label}</div>
              <div style={{ fontWeight:600, color:'var(--navy-800)', fontSize:'0.875rem' }}>{value}</div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}