import React, { useState, useEffect } from 'react';
import { FileText, CheckCircle2, XCircle, Clock, Upload } from 'lucide-react';
import { documentApi } from '../../api/services';
import { PageLoader, Alert, Card, SectionHeader } from '../../components/common';
import { fmtDateTime, getDocTypeLabel } from '../../utils/helpers';

const STATUS_ICON = {
  VERIFIED: <CheckCircle2 size={16} style={{ color:'var(--green-600)' }} />,
  REJECTED:  <XCircle     size={16} style={{ color:'var(--red-500)' }} />,
  PENDING:   <Clock       size={16} style={{ color:'var(--yellow-500)' }} />,
};

export default function DocumentsPage() {
  const [docs,    setDocs]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    documentApi.getMyDocuments()
      .then(r => setDocs(r.data))
      .catch(() => setError('Failed to load documents.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  // Group by application
  const byApp = {};
  docs.forEach(d => {
    if (!byApp[d.applicationId]) byApp[d.applicationId] = [];
    byApp[d.applicationId].push(d);
  });

  return (
    <div>
      <SectionHeader title="My Documents" subtitle={`${docs.length} document${docs.length !== 1 ? 's' : ''} uploaded across all applications`} />

      {error && <Alert type="error">{error}</Alert>}

      {docs.length === 0 ? (
        <Card>
          <div className="empty-state">
            <Upload size={40} style={{ color:'var(--navy-300)' }} />
            <div className="empty-state-title">No documents uploaded</div>
            <p style={{ fontSize:'0.875rem', color:'var(--navy-500)' }}>
              Documents are uploaded from the application detail page.
            </p>
          </div>
        </Card>
      ) : (
        Object.entries(byApp).map(([appId, appDocs]) => (
          <Card key={appId} style={{ marginBottom:'1.25rem' }}>
            <div style={{ padding:'1rem 1.25rem', borderBottom:'1px solid var(--navy-100)', fontWeight:600, fontSize:'0.9375rem', color:'var(--navy-700)' }}>
              Application #{appId}
            </div>
            <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
              <table>
                <thead>
                  <tr>
                    <th>Document Type</th>
                    <th>File Name</th>
                    <th>Size</th>
                    <th>Status</th>
                    <th>Uploaded</th>
                    <th>Remarks</th>
                  </tr>
                </thead>
                <tbody>
                  {appDocs.map(doc => (
                    <tr key={doc.id}>
                      <td>
                        <div style={{ display:'flex', alignItems:'center', gap:'0.5rem', fontWeight:500 }}>
                          {STATUS_ICON[doc.status] || <FileText size={15} />}
                          {getDocTypeLabel(doc.documentType)}
                        </div>
                      </td>
                      <td style={{ color:'var(--navy-600)', maxWidth:200, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{doc.fileName}</td>
                      <td style={{ color:'var(--navy-500)' }}>{doc.fileSize ? `${(doc.fileSize/1024/1024).toFixed(2)} MB` : '—'}</td>
                      <td><span className={`badge badge-${doc.status.toLowerCase()}`}>{doc.status}</span></td>
                      <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem', whiteSpace:'nowrap' }}>{fmtDateTime(doc.uploadedAt)}</td>
                      <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem' }}>{doc.verificationRemarks || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        ))
      )}
    </div>
  );
}
