import React, { useState, useEffect } from 'react';
import { CheckCircle2, XCircle } from 'lucide-react';
import { adminApi } from '../../api/services';
import { PageLoader, Alert, Card, SectionHeader } from '../../components/common';
import { fmtCurrency, fmtDateTime } from '../../utils/helpers';

export default function AdminDecisionsPage() {
  const [decisions, setDecisions] = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState('');

  useEffect(() => {
    adminApi.getDecisions()
      .then(r => setDecisions(r.data))
      .catch(() => setError('Failed to load decisions.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  return (
    <div>
      <SectionHeader title="All Decisions" subtitle={`${decisions.length} decision${decisions.length !== 1 ? 's' : ''} recorded`} />

      {error && <Alert type="error">{error}</Alert>}

      <Card>
        <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
          <table>
            <thead>
              <tr>
                <th>Decision</th><th>Application</th><th>Admin</th>
                <th>Approved Amount</th><th>Rate</th><th>Tenure</th><th>Remarks</th><th>Date</th>
              </tr>
            </thead>
            <tbody>
              {decisions.length === 0 ? (
                <tr><td colSpan={8} style={{ textAlign:'center', padding:'3rem', color:'var(--navy-400)' }}>No decisions recorded yet</td></tr>
              ) : decisions.map(d => (
                <tr key={d.id}>
                  <td>
                    <div style={{ display:'flex', alignItems:'center', gap:6, fontWeight:600, color: d.decision==='APPROVED' ? 'var(--green-700)' : 'var(--red-600)' }}>
                      {d.decision === 'APPROVED' ? <CheckCircle2 size={15}/> : <XCircle size={15}/>}
                      {d.decision}
                    </div>
                  </td>
                  <td style={{ fontWeight:500 }}>#{d.applicationId}</td>
                  <td style={{ color:'var(--navy-600)' }}>#{d.adminId}</td>
                  <td style={{ fontWeight:600 }}>{d.approvedAmount ? fmtCurrency(d.approvedAmount) : '—'}</td>
                  <td style={{ color:'var(--navy-600)' }}>{d.interestRate ? `${d.interestRate}%` : '—'}</td>
                  <td style={{ color:'var(--navy-600)' }}>{d.tenureMonths ? `${d.tenureMonths} mo` : '—'}</td>
                  <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem', maxWidth:200 }}>{d.remarks || '—'}</td>
                  <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem', whiteSpace:'nowrap' }}>{fmtDateTime(d.decidedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
