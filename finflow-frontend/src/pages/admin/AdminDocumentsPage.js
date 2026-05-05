import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Search, ExternalLink } from 'lucide-react';
import { documentApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, Card, SectionHeader, Modal, Spinner } from '../../components/common';
import { fmtDateTime, getDocTypeLabel } from '../../utils/helpers';

export default function AdminDocumentsPage() {
  const navigate = useNavigate();
  const [docs,    setDocs]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');
  const [search,  setSearch]  = useState('');

  const [verifyOpen,    setVerifyOpen]    = useState(false);
  const [verifyDoc,     setVerifyDoc]     = useState(null);
  const [verifyRemarks, setVerifyRemarks] = useState('');
  const [verifying,     setVerifying]     = useState(false);

  const load = () => {
    documentApi.getPending()
      .then(r => setDocs(r.data))
      .catch(() => setError('Failed to load pending documents.'))
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);

  const openVerify = (doc) => { setVerifyDoc(doc); setVerifyRemarks(''); setVerifyOpen(true); };

  const handleVerify = async (approved) => {
    setVerifying(true);
    try {
      await documentApi.verify(verifyDoc.id, approved, verifyRemarks);
      setVerifyOpen(false);
      setSuccess(`Document ${approved ? 'verified' : 'rejected'}.`);
      load();
    } catch(err) { setError(extractError(err)); }
    finally { setVerifying(false); }
  };

  const filtered = docs.filter(d => {
    const q = search.toLowerCase();
    return !q || d.documentType?.toLowerCase().includes(q) || String(d.applicationId).includes(q) || d.fileName?.toLowerCase().includes(q);
  });

  if (loading) return <PageLoader />;

  return (
    <div>
      <SectionHeader title="Document Queue" subtitle={`${docs.length} document${docs.length !== 1 ? 's' : ''} pending verification`} />

      {error   && <Alert type="error"   onClose={() => setError('')}   style={{ marginBottom:'1rem' }}>{error}</Alert>}
      {success && <Alert type="success" onClose={() => setSuccess('')} style={{ marginBottom:'1rem' }}>{success}</Alert>}

      <div style={{ position:'relative', marginBottom:'1.25rem', maxWidth:400 }}>
        <Search size={15} style={{ position:'absolute', left:'0.75rem', top:'50%', transform:'translateY(-50%)', color:'var(--navy-400)' }} />
        <input className="form-input" style={{ paddingLeft:'2.25rem' }} placeholder="Search by type, app ID, filename…"
          value={search} onChange={e => setSearch(e.target.value)} />
      </div>

      {filtered.length === 0 ? (
        <Card>
          <div className="empty-state">
            <CheckCircle2 size={40} style={{ color:'var(--green-400)' }} />
            <div className="empty-state-title">{search ? 'No documents match your search' : 'All documents reviewed!'}</div>
            <p style={{ fontSize:'0.875rem' }}>{search ? 'Try a different search term.' : 'Great job — the queue is clear.'}</p>
          </div>
        </Card>
      ) : (
        <Card>
          <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
            <table>
              <thead>
                <tr>
                  <th>Document Type</th><th>Application</th><th>Filename</th>
                  <th>Size</th><th>Uploaded</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(doc => (
                  <tr key={doc.id}>
                    <td style={{ fontWeight:600 }}>{getDocTypeLabel(doc.documentType)}</td>
                    <td>
                      <button className="btn btn-ghost btn-sm" style={{ padding:'2px 6px', gap:4 }}
                        onClick={() => navigate(`/admin/applications/${doc.applicationId}`)}>
                        #{doc.applicationId} <ExternalLink size={11}/>
                      </button>
                    </td>
                    <td style={{ color:'var(--navy-600)', maxWidth:180, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{doc.fileName}</td>
                    <td style={{ color:'var(--navy-500)' }}>{doc.fileSize ? `${(doc.fileSize/1024/1024).toFixed(2)} MB` : '—'}</td>
                    <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem', whiteSpace:'nowrap' }}>{fmtDateTime(doc.uploadedAt)}</td>
                    <td>
                      <div style={{ display:'flex', gap:'0.5rem' }}>
                        <button className="btn btn-success btn-sm" onClick={() => openVerify(doc)}>
                          <CheckCircle2 size={13}/> Verify
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => { setVerifyDoc(doc); setVerifyRemarks(''); handleVerify(false); }}>
                          <XCircle size={13}/> Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={verifyOpen} onClose={() => setVerifyOpen(false)}
        title={`Review: ${verifyDoc?.documentType?.replace(/_/g,' ')}`}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setVerifyOpen(false)} disabled={verifying}>Cancel</button>
            <button className="btn btn-danger"  onClick={() => handleVerify(false)} disabled={verifying}>
              {verifying ? <Spinner size="sm"/> : <XCircle size={14}/>} Reject
            </button>
            <button className="btn btn-success" onClick={() => handleVerify(true)}  disabled={verifying}>
              {verifying ? <Spinner size="sm"/> : <CheckCircle2 size={14}/>} Verify
            </button>
          </>
        }
      >
        <p style={{ fontSize:'0.875rem', color:'var(--navy-600)', marginBottom:'1rem' }}>
          File: <strong>{verifyDoc?.fileName}</strong> · App #{verifyDoc?.applicationId}
        </p>
        <div className="form-group">
          <label className="form-label">Remarks (optional)</label>
          <textarea className="form-textarea" value={verifyRemarks} onChange={e => setVerifyRemarks(e.target.value)}
            placeholder="Add any remarks…" rows={3} />
        </div>
      </Modal>
    </div>
  );
}
