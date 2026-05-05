import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ChevronLeft, CheckCircle2, XCircle, Clock, ShieldCheck, TrendingUp, Activity } from 'lucide-react';
import { applicationApi, documentApi, adminApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, StatusBadge, Card, Modal, Spinner } from '../../components/common';
import { fmtCurrency, fmtDate, fmtDateTime, getLoanTypeLabel, getEmpTypeLabel, DOCUMENT_TYPES } from '../../utils/helpers';

function InfoRow({ label, value }) {
  return (
    <div style={{ display:'flex', padding:'0.5rem 0', borderBottom:'1px solid var(--navy-100)', fontSize:'0.875rem' }}>
      <span style={{ width:200, color:'var(--navy-500)', flexShrink:0 }}>{label}</span>
      <span style={{ fontWeight:500, color:'var(--navy-900)' }}>{value ?? '—'}</span>
    </div>
  );
}

const DOC_STATUS_ICON = {
  VERIFIED: <CheckCircle2 size={15} style={{ color:'var(--green-600)' }} />,
  REJECTED:  <XCircle     size={15} style={{ color:'var(--red-500)' }} />,
  PENDING:   <Clock       size={15} style={{ color:'var(--yellow-500)' }} />,
};

export default function AdminApplicationDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [app,     setApp]     = useState(null);
  const [docs,    setDocs]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  // Verify doc modal
  const [verifyOpen,   setVerifyOpen]   = useState(false);
  const [verifyDoc,    setVerifyDoc]    = useState(null);
  const [verifyRemarks,setVerifyRemarks]= useState('');
  const [verifying,    setVerifying]    = useState(false);

  // Decision modal
  const [decisionOpen,    setDecisionOpen]    = useState(false);
  const [decisionType,    setDecisionType]    = useState('APPROVED');
  const [decisionRemarks, setDecisionRemarks] = useState('');
  const [approvedAmount,  setApprovedAmount]  = useState('');
  const [interestRate,    setInterestRate]    = useState('');
  const [tenureMonths,    setTenureMonths]    = useState('');
  const [deciding,        setDeciding]        = useState(false);

  const load = useCallback(async () => {
    try {
      const [appRes, docRes] = await Promise.all([
        applicationApi.getById(id),
        documentApi.getByApplication(id),
      ]);
      setApp(appRes.data);
      setDocs(docRes.data);
    } catch { setError('Failed to load application.'); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const openVerify = (doc) => { setVerifyDoc(doc); setVerifyRemarks(''); setVerifyOpen(true); };

  const handleVerify = async (approved) => {
    setVerifying(true);
    try {
      await documentApi.verify(verifyDoc.id, approved, verifyRemarks);
      setVerifyOpen(false);
      setSuccess(`Document ${approved ? 'verified' : 'rejected'} successfully.`);
      await load();
    } catch(err) { setError(extractError(err)); }
    finally { setVerifying(false); }
  };

  const handleDecision = async () => {
    setDeciding(true); setError('');
    try {
      await adminApi.makeDecision(id, {
        decision: decisionType,
        remarks:  decisionRemarks,
        approvedAmount: approvedAmount ? Number(approvedAmount) : undefined,
        interestRate:   interestRate   ? Number(interestRate)   : undefined,
        tenureMonths:   tenureMonths   ? Number(tenureMonths)   : undefined,
      });
      setDecisionOpen(false);
      setSuccess(`Application ${decisionType === 'APPROVED' ? 'approved' : 'rejected'} successfully. Applicant has been notified.`);
      await load();
    } catch(err) { setError(extractError(err)); }
    finally { setDeciding(false); }
  };

  if (loading) return <PageLoader />;
  if (!app)    return <Alert type="error">Application not found.</Alert>;

  const docMap = {};
  docs.forEach(d => { docMap[d.documentType] = d; });
  const canDecide = app.status === 'DOCS_VERIFIED';

  return (
    <div style={{ maxWidth: 900 }}>
      {/* Header */}
      <div style={{ display:'flex', alignItems:'center', gap:'1rem', marginBottom:'1.5rem', flexWrap:'wrap' }}>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/admin/applications')} style={{ padding:'6px 8px' }}>
          <ChevronLeft size={16} />
        </button>
        <div style={{ flex:1 }}>
          <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', flexWrap:'wrap' }}>
            <h2>Application #{app.id}</h2>
            <StatusBadge status={app.status} />
          </div>
          <div style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:2 }}>
            {app.fullName} · {getLoanTypeLabel(app.loanType)} · {fmtDate(app.createdAt)}
          </div>
        </div>
        {canDecide && (
          <button className="btn btn-primary" onClick={() => setDecisionOpen(true)}>
            <ShieldCheck size={15} /> Make Decision
          </button>
        )}
        {!canDecide && app.status !== 'DRAFT' && app.status !== 'SUBMITTED' && (
          <span style={{ fontSize:'0.8125rem', color:'var(--navy-500)' }}>
            {['APPROVED','REJECTED','CLOSED'].includes(app.status) ? 'Decision recorded' : 'Awaiting documents'}
          </span>
        )}
        <Link to={`/admin/applications/${app.id}/credit-score`} className="btn btn-secondary btn-sm">
          <TrendingUp size={13} /> Credit Score
        </Link>
      </div>

      {error   && <Alert type="error"   onClose={() => setError('')}   style={{ marginBottom:'1rem' }}>{error}</Alert>}
      {success && <Alert type="success" onClose={() => setSuccess('')} style={{ marginBottom:'1rem' }}>{success}</Alert>}

      {!canDecide && app.status === 'SUBMITTED' && (
        <Alert type="info" style={{ marginBottom:'1.25rem' }}>
          This application is submitted. Verify all 4 documents below to enable the decision action.
        </Alert>
      )}

      {/* EMI strip */}
      {app.emiAmount && (
        <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit, minmax(160px, 1fr))', gap:'0.75rem', marginBottom:'1.25rem' }}>
          {[
            { l:'Loan Amount',    v: fmtCurrency(app.loanAmount) },
            { l:'Interest Rate',  v: app.effectiveInterestRate ? `${app.effectiveInterestRate}% p.a.` : '—' },
            { l:'Monthly EMI',    v: fmtCurrency(app.emiAmount) },
            { l:'Tenure',         v: app.tenureMonths ? `${app.tenureMonths} months` : '—' },
          ].map(s => (
            <Card key={s.l} style={{ padding:'0.875rem 1rem', textAlign:'center' }}>
              <div style={{ fontSize:'1.125rem', fontWeight:700, color:'var(--teal-700)' }}>{s.v}</div>
              <div style={{ fontSize:'0.75rem', color:'var(--navy-500)', marginTop:2 }}>{s.l}</div>
            </Card>
          ))}
        </div>
      )}

      <div style={{ display:'grid', gridTemplateColumns:'1fr 340px', gap:'1.25rem' }}>
        {/* Details */}
        <Card style={{ padding:'1.25rem 1.5rem' }}>
          <div style={{ fontWeight:600, marginBottom:'1rem' }}>Applicant Details</div>
          <InfoRow label="Full Name"      value={app.fullName} />
          <InfoRow label="Email"          value={app.email} />
          <InfoRow label="Phone"          value={app.phone} />
          <InfoRow label="Date of Birth"  value={app.dateOfBirth} />
          <InfoRow label="Address"        value={app.address} />
          <InfoRow label="Employment"     value={getEmpTypeLabel(app.employmentType)} />
          <InfoRow label="Employer"       value={app.employerName} />
          <InfoRow label="Monthly Income" value={fmtCurrency(app.monthlyIncome)} />
          <InfoRow label="Loan Type"      value={getLoanTypeLabel(app.loanType)} />
          <InfoRow label="Loan Amount"    value={fmtCurrency(app.loanAmount)} />
          <InfoRow label="Tenure"         value={app.tenureMonths ? `${app.tenureMonths} months` : null} />
          <InfoRow label="Purpose"        value={app.purpose} />
          {app.propertyValue   && <InfoRow label="Property Value"  value={fmtCurrency(app.propertyValue)} />}
          {app.vehicleModel    && <InfoRow label="Vehicle Model"   value={app.vehicleModel} />}
          {app.institutionName && <InfoRow label="Institution"     value={app.institutionName} />}
          {app.goldMarketValue && <InfoRow label="Gold Value"      value={fmtCurrency(app.goldMarketValue)} />}
          {app.coApplicantName && <InfoRow label="Co-applicant"    value={app.coApplicantName} />}
          {app.submittedAt     && <InfoRow label="Submitted At"    value={fmtDateTime(app.submittedAt)} />}
          {app.remarks         && <InfoRow label="Remarks"         value={app.remarks} />}
        </Card>

        {/* Documents column */}
        <Card style={{ padding:'1.25rem' }}>
          <div style={{ fontWeight:600, marginBottom:'1rem' }}>Document Verification</div>
          <div style={{ display:'flex', flexDirection:'column', gap:'0.75rem' }}>
            {DOCUMENT_TYPES.map(dt => {
              const doc = docMap[dt.value];
              return (
                <div key={dt.value} style={{ border:'1px solid var(--navy-200)', borderRadius:'var(--radius-lg)', padding:'0.875rem', background: doc ? 'var(--navy-50)' : '#fff' }}>
                  <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:'0.5rem' }}>
                    <div style={{ flex:1 }}>
                      <div style={{ fontSize:'0.875rem', fontWeight:600, color:'var(--navy-800)', display:'flex', alignItems:'center', gap:6 }}>
                        {doc ? (DOC_STATUS_ICON[doc.status] || null) : <Clock size={14} style={{ color:'var(--navy-300)' }} />}
                        {dt.label}
                      </div>
                      {doc && (
                        <div style={{ fontSize:'0.75rem', color:'var(--navy-500)', marginTop:3 }}>
                          {doc.fileName} · <span className={`badge badge-${doc.status.toLowerCase()}`}>{doc.status}</span>
                        </div>
                      )}
                      {!doc && <div style={{ fontSize:'0.75rem', color:'var(--navy-400)', marginTop:2 }}>Not uploaded</div>}
                    </div>
                    {doc && doc.status === 'PENDING' && (
                      <button className="btn btn-secondary btn-sm" onClick={() => openVerify(doc)} style={{ flexShrink:0 }}>
                        Review
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      </div>

      {/* Verify Modal */}
      <Modal open={verifyOpen} onClose={() => setVerifyOpen(false)}
        title={`Review: ${verifyDoc?.documentType?.replace(/_/g,' ')}`}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setVerifyOpen(false)} disabled={verifying}>Cancel</button>
            <button className="btn btn-danger"    onClick={() => handleVerify(false)}  disabled={verifying}>
              {verifying ? <Spinner size="sm" /> : <XCircle size={14}/>} Reject
            </button>
            <button className="btn btn-success"   onClick={() => handleVerify(true)}   disabled={verifying}>
              {verifying ? <Spinner size="sm" /> : <CheckCircle2 size={14}/>} Verify
            </button>
          </>
        }
      >
        <p style={{ fontSize:'0.875rem', color:'var(--navy-600)', marginBottom:'1rem' }}>
          File: <strong>{verifyDoc?.fileName}</strong>
        </p>
        <div className="form-group">
          <label className="form-label">Remarks (optional)</label>
          <textarea className="form-textarea" value={verifyRemarks} onChange={e => setVerifyRemarks(e.target.value)}
            placeholder="Add any remarks about this document…" rows={3} />
        </div>
      </Modal>

      {/* Decision Modal */}
      <Modal open={decisionOpen} onClose={() => setDecisionOpen(false)}
        title="Make a Decision"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDecisionOpen(false)} disabled={deciding}>Cancel</button>
            <button
              className={`btn ${decisionType === 'APPROVED' ? 'btn-success' : 'btn-danger'}`}
              onClick={handleDecision} disabled={deciding}>
              {deciding ? <><Spinner size="sm" /> Processing...</> : decisionType === 'APPROVED' ? <><CheckCircle2 size={14}/> Approve</> : <><XCircle size={14}/> Reject</>}
            </button>
          </>
        }
      >
        <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
          <div className="form-group">
            <label className="form-label">Decision <span className="required">*</span></label>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'0.625rem' }}>
              {['APPROVED','REJECTED'].map(d => (
                <button key={d} type="button"
                  style={{ padding:'0.75rem', borderRadius:'var(--radius)', border:`2px solid ${decisionType===d ? (d==='APPROVED'?'var(--green-500)':'var(--red-500)') : 'var(--navy-200)'}`, background: decisionType===d ? (d==='APPROVED'?'var(--green-50)':'var(--red-50)') : '#fff', cursor:'pointer', fontWeight:600, fontSize:'0.9rem', color: decisionType===d ? (d==='APPROVED'?'var(--green-700)':'var(--red-700)') : 'var(--navy-600)', display:'flex', alignItems:'center', justifyContent:'center', gap:6 }}
                  onClick={() => setDecisionType(d)}>
                  {d === 'APPROVED' ? <CheckCircle2 size={15}/> : <XCircle size={15}/>} {d}
                </button>
              ))}
            </div>
          </div>
          {decisionType === 'APPROVED' && (
            <>
              <div className="form-grid-2">
                <div className="form-group">
                  <label className="form-label">Approved Amount (₹)</label>
                  <input className="form-input" type="number" value={approvedAmount} onChange={e => setApprovedAmount(e.target.value)} placeholder={app.loanAmount} />
                </div>
                <div className="form-group">
                  <label className="form-label">Interest Rate (%)</label>
                  <input className="form-input" type="number" step="0.1" value={interestRate} onChange={e => setInterestRate(e.target.value)} placeholder={app.effectiveInterestRate || '10.5'} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Tenure (Months)</label>
                <input className="form-input" type="number" value={tenureMonths} onChange={e => setTenureMonths(e.target.value)} placeholder={app.tenureMonths} />
              </div>
            </>
          )}
          <div className="form-group">
            <label className="form-label">Remarks</label>
            <textarea className="form-textarea" value={decisionRemarks} onChange={e => setDecisionRemarks(e.target.value)}
              placeholder={decisionType === 'APPROVED' ? 'Good credit profile. Loan approved.' : 'Reason for rejection…'} rows={3} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
