import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ChevronLeft, Upload, Send, RefreshCw, CheckCircle2, XCircle, Clock, AlertCircle, Edit2, FileText, Download, Activity, TrendingUp } from 'lucide-react';
import { applicationApi, documentApi } from '../../api/services';
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
  VERIFIED: <CheckCircle2 size={16} style={{ color:'var(--green-600)' }} />,
  REJECTED:  <XCircle size={16} style={{ color:'var(--red-500)' }} />,
  PENDING:   <Clock size={16} style={{ color:'var(--yellow-500)' }} />,
};

export default function ApplicationDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [app,     setApp]     = useState(null);
  const [docs,    setDocs]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  // Upload modal state
  const [uploadOpen,    setUploadOpen]    = useState(false);
  const [uploadType,    setUploadType]    = useState('');
  const [uploadFile,    setUploadFile]    = useState(null);
  const [uploading,     setUploading]     = useState(false);
  const [uploadError,   setUploadError]   = useState('');

  // Submit state
  const [submitting, setSubmitting] = useState(false);

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

  const handleSubmit = async () => {
    setError(''); setSubmitting(true);
    try {
      const r = await applicationApi.submit(id);
      setSuccess(`Application submitted! Estimated EMI: ${fmtCurrency(r.data.application?.emiAmount)}/month`);
      setApp(r.data.application);
    } catch(err) { setError(extractError(err)); }
    finally { setSubmitting(false); }
  };

  const openUpload = (docType) => { setUploadType(docType); setUploadFile(null); setUploadError(''); setUploadOpen(true); };

  const handleUpload = async () => {
    if (!uploadFile) { setUploadError('Please select a file.'); return; }
    setUploading(true); setUploadError('');
    try {
      await documentApi.upload(id, uploadType, uploadFile);
      setUploadOpen(false);
      setSuccess(`${uploadType.replace('_',' ')} uploaded successfully.`);
      await load();
    } catch(err) { setUploadError(extractError(err)); }
    finally { setUploading(false); }
  };

  if (loading) return <PageLoader />;
  if (!app)    return <Alert type="error">Application not found.</Alert>;

  const docMap = {};
  docs.forEach(d => { docMap[d.documentType] = d; });
  const allUploaded  = DOCUMENT_TYPES.every(t => docMap[t.value]);
  const isDraft      = app.status === 'DRAFT';
  const canEdit      = isDraft;
  const canSubmit    = isDraft && allUploaded;

  return (
    <div style={{ maxWidth: 900 }}>
      {/* Header */}
      <div style={{ display:'flex', alignItems:'center', gap:'1rem', marginBottom:'1.5rem', flexWrap:'wrap' }}>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/applications')} style={{ padding:'6px 8px' }}>
          <ChevronLeft size={16} />
        </button>
        <div style={{ flex:1 }}>
          <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', flexWrap:'wrap' }}>
            <h2>Application #{app.id}</h2>
            <StatusBadge status={app.status} />
          </div>
          <div style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:2 }}>
            {getLoanTypeLabel(app.loanType)} · Created {fmtDate(app.createdAt)}
          </div>
        </div>
        {canEdit && (
          <Link to={`/applications/${id}/edit`} className="btn btn-secondary btn-sm">
            <Edit2 size={14} /> Edit
          </Link>
        )}
        <Link to={`/applications/${id}/status`} className="btn btn-secondary btn-sm">
          <Activity size={14} /> Track Status
        </Link>
        <Link to={`/applications/${id}/credit-score`} className="btn btn-secondary btn-sm">
          <TrendingUp size={14} /> Credit Score
        </Link>
      </div>

      {error   && <Alert type="error"   onClose={() => setError('')}   style={{ marginBottom:'1rem' }}>{error}</Alert>}
      {success && <Alert type="success" onClose={() => setSuccess('')} style={{ marginBottom:'1rem' }}>{success}</Alert>}

      <div style={{ display:'grid', gridTemplateColumns:'1fr 360px', gap:'1.25rem' }}>
        {/* Left column */}
        <div style={{ display:'flex', flexDirection:'column', gap:'1.25rem' }}>

          {/* Loan summary */}
          {(app.emiAmount || app.effectiveInterestRate) && (
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:'0.75rem' }}>
              {[
                { label:'Loan Amount',    value: fmtCurrency(app.loanAmount) },
                { label:'Interest Rate',  value: app.effectiveInterestRate ? `${app.effectiveInterestRate}% p.a.` : '—' },
                { label:'Monthly EMI',    value: fmtCurrency(app.emiAmount) },
              ].map(s => (
                <Card key={s.label} style={{ padding:'0.875rem 1rem', textAlign:'center' }}>
                  <div style={{ fontSize:'1.25rem', fontWeight:700, color:'var(--teal-700)' }}>{s.value}</div>
                  <div style={{ fontSize:'0.75rem', color:'var(--navy-500)', marginTop:2 }}>{s.label}</div>
                </Card>
              ))}
            </div>
          )}

          {/* Application details */}
          <Card style={{ padding:'1.25rem 1.5rem' }}>
            <div style={{ fontWeight:600, marginBottom:'1rem' }}>Application Details</div>
            <InfoRow label="Full Name"        value={app.fullName} />
            <InfoRow label="Email"            value={app.email} />
            <InfoRow label="Phone"            value={app.phone} />
            <InfoRow label="Date of Birth"    value={app.dateOfBirth} />
            <InfoRow label="Address"          value={app.address} />
            <InfoRow label="Employment"       value={getEmpTypeLabel(app.employmentType)} />
            <InfoRow label="Employer"         value={app.employerName} />
            <InfoRow label="Monthly Income"   value={fmtCurrency(app.monthlyIncome)} />
            <InfoRow label="Loan Type"        value={getLoanTypeLabel(app.loanType)} />
            <InfoRow label="Loan Amount"      value={fmtCurrency(app.loanAmount)} />
            <InfoRow label="Tenure"           value={app.tenureMonths ? `${app.tenureMonths} months` : null} />
            <InfoRow label="Purpose"          value={app.purpose} />
            {app.propertyValue  && <InfoRow label="Property Value"  value={fmtCurrency(app.propertyValue)} />}
            {app.vehicleModel   && <InfoRow label="Vehicle Model"   value={app.vehicleModel} />}
            {app.institutionName && <InfoRow label="Institution"    value={app.institutionName} />}
            {app.goldMarketValue && <InfoRow label="Gold Value"     value={fmtCurrency(app.goldMarketValue)} />}
            {app.coApplicantName && <InfoRow label="Co-applicant"   value={app.coApplicantName} />}
            {app.submittedAt    && <InfoRow label="Submitted At"    value={fmtDateTime(app.submittedAt)} />}
            {app.remarks        && <InfoRow label="Admin Remarks"   value={app.remarks} />}
            {app.eligibilityReason && <InfoRow label="Eligibility"  value={app.eligibilityReason} />}
          </Card>
        </div>

        {/* Right column */}
        <div style={{ display:'flex', flexDirection:'column', gap:'1.25rem' }}>
          {/* Submit action */}
          {isDraft && (
            <Card style={{ padding:'1.25rem' }}>
              <div style={{ fontWeight:600, marginBottom:'0.5rem' }}>Ready to Submit?</div>
              <p style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginBottom:'1rem', lineHeight:1.6 }}>
                Upload all 4 required documents below, then submit your application for review.
              </p>
              {!allUploaded && (
                <Alert type="warning" style={{ marginBottom:'1rem' }}>
                  <span style={{ fontSize:'0.8rem' }}>All 4 documents must be uploaded before submitting.</span>
                </Alert>
              )}
              <button className="btn btn-primary w-full" onClick={handleSubmit} disabled={!canSubmit || submitting}>
                {submitting ? <><Spinner size="sm" /> Submitting...</> : <><Send size={15} /> Submit Application</>}
              </button>
            </Card>
          )}

          {/* Documents */}
          <Card style={{ padding:'1.25rem' }}>
            <div style={{ fontWeight:600, marginBottom:'1rem', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
              <span>Documents</span>
              <button className="btn btn-ghost btn-sm" onClick={load} style={{ padding:'4px 6px' }}><RefreshCw size={13} /></button>
            </div>
            <div style={{ display:'flex', flexDirection:'column', gap:'0.75rem' }}>
              {DOCUMENT_TYPES.map(dt => {
                const doc = docMap[dt.value];
                return (
                  <div key={dt.value} style={{ border:'1px solid var(--navy-200)', borderRadius:'var(--radius-lg)', padding:'0.875rem', background: doc ? 'var(--navy-50)' : '#fff' }}>
                    <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:'0.5rem' }}>
                      <div style={{ flex:1 }}>
                        <div style={{ fontSize:'0.875rem', fontWeight:600, color:'var(--navy-800)', display:'flex', alignItems:'center', gap:'0.375rem' }}>
                          {doc ? DOC_STATUS_ICON[doc.status] : <AlertCircle size={15} style={{ color:'var(--navy-300)' }} />}
                          {dt.label}
                        </div>
                        <div style={{ fontSize:'0.75rem', color:'var(--navy-500)', marginTop:2 }}>{dt.hint}</div>
                        {doc && (
                          <div style={{ fontSize:'0.75rem', marginTop:4, display:'flex', alignItems:'center', gap:4 }}>
                            <span className={`badge badge-${doc.status.toLowerCase()}`}>{doc.status}</span>
                            <span style={{ color:'var(--navy-400)' }}>· {doc.fileName}</span>
                          </div>
                        )}
                      </div>
                      {(isDraft || (doc?.status === 'REJECTED')) && (
                        <button className="btn btn-secondary btn-sm" onClick={() => openUpload(dt.value)} style={{ flexShrink:0 }}>
                          <Upload size={12} /> {doc ? 'Replace' : 'Upload'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
            <p style={{ fontSize:'0.75rem', color:'var(--navy-400)', marginTop:'0.75rem' }}>Accepted: PDF, JPEG, PNG, WEBP · Max 10 MB</p>
          </Card>
        </div>
      </div>

      {/* Upload Modal */}
      <Modal open={uploadOpen} onClose={() => setUploadOpen(false)}
        title={`Upload ${DOCUMENT_TYPES.find(t=>t.value===uploadType)?.label || ''}`}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setUploadOpen(false)} disabled={uploading}>Cancel</button>
            <button className="btn btn-primary" onClick={handleUpload} disabled={uploading || !uploadFile}>
              {uploading ? <><Spinner size="sm" /> Uploading...</> : <><Upload size={14} /> Upload</>}
            </button>
          </>
        }
      >
        {uploadError && <Alert type="error" style={{ marginBottom:'1rem' }}>{uploadError}</Alert>}
        <label className="file-drop" htmlFor="file-input" style={{ display:'block' }}>
          <input id="file-input" type="file" accept=".pdf,.jpg,.jpeg,.png,.webp" onChange={e => setUploadFile(e.target.files[0])} />
          <FileText size={32} style={{ color:'var(--navy-300)', marginBottom:'0.5rem' }} />
          <div style={{ fontWeight:600, color:'var(--navy-700)', marginBottom:4 }}>{uploadFile ? uploadFile.name : 'Click to select file'}</div>
          <div style={{ fontSize:'0.8125rem', color:'var(--navy-400)' }}>PDF, JPEG, PNG or WEBP · Max 10 MB</div>
        </label>
        {uploadFile && (
          <div style={{ marginTop:'0.75rem', fontSize:'0.8125rem', color:'var(--navy-600)', background:'var(--navy-50)', padding:'0.625rem 0.875rem', borderRadius:'var(--radius)', display:'flex', gap:'0.5rem' }}>
            <FileText size={14} style={{ flexShrink:0, marginTop:1 }} />
            {uploadFile.name} · {(uploadFile.size / 1024 / 1024).toFixed(2)} MB
          </div>
        )}
      </Modal>
    </div>
  );
}
