import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ChevronRight } from 'lucide-react';
import { applicationApi } from '../../api/services';
import { PageLoader, Alert, StatusBadge, Card, SectionHeader } from '../../components/common';
import { fmtCurrency, fmtDate, getLoanTypeLabel } from '../../utils/helpers';

export default function AdminApplicationsPage() {
  const navigate = useNavigate();
  const [apps,    setApps]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [search,  setSearch]  = useState('');
  const [status,  setStatus]  = useState('ALL');

  useEffect(() => {
    applicationApi.getAll()
      .then(r => setApps(r.data))
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false));
  }, []);

  const STATUSES = ['ALL','DRAFT','SUBMITTED','DOCS_PENDING','DOCS_VERIFIED','UNDER_REVIEW','APPROVED','REJECTED'];

  const filtered = apps.filter(a => {
    const ms = status === 'ALL' || a.status === status;
    const q  = search.toLowerCase();
    const mq = !q || getLoanTypeLabel(a.loanType).toLowerCase().includes(q)
                  || String(a.id).includes(q) || a.fullName?.toLowerCase().includes(q)
                  || a.email?.toLowerCase().includes(q);
    return ms && mq;
  });

  if (loading) return <PageLoader />;

  return (
    <div>
      <SectionHeader title="All Applications" subtitle={`${apps.length} total · ${filtered.length} shown`} />

      {error && <Alert type="error" style={{ marginBottom:'1rem' }}>{error}</Alert>}

      <div style={{ display:'flex', gap:'0.75rem', marginBottom:'1.25rem', flexWrap:'wrap' }}>
        <div style={{ position:'relative', flex:1, minWidth:220 }}>
          <Search size={15} style={{ position:'absolute', left:'0.75rem', top:'50%', transform:'translateY(-50%)', color:'var(--navy-400)' }} />
          <input className="form-input" style={{ paddingLeft:'2.25rem' }} placeholder="Search by ID, name, email, loan type…"
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div style={{ display:'flex', gap:'0.375rem', flexWrap:'wrap' }}>
          {STATUSES.map(s => (
            <button key={s} className={`btn btn-sm ${status === s ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setStatus(s)}>
              {s === 'ALL' ? 'All' : s.replace(/_/g,' ')}
            </button>
          ))}
        </div>
      </div>

      <Card>
        <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Applicant</th><th>Loan Type</th><th>Amount</th>
                <th>Tenure</th><th>EMI</th><th>Status</th><th>Date</th><th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={9} style={{ textAlign:'center', padding:'3rem', color:'var(--navy-400)' }}>No applications found</td></tr>
              ) : filtered.map(a => (
                <tr key={a.id} style={{ cursor:'pointer' }} onClick={() => navigate(`/admin/applications/${a.id}`)}>
                  <td style={{ fontWeight:600, color:'var(--navy-400)', fontSize:'0.8125rem' }}>#{a.id}</td>
                  <td>
                    <div style={{ fontWeight:500 }}>{a.fullName || '—'}</div>
                    <div style={{ fontSize:'0.75rem', color:'var(--navy-400)' }}>{a.email}</div>
                  </td>
                  <td>{getLoanTypeLabel(a.loanType)}</td>
                  <td style={{ fontWeight:600 }}>{fmtCurrency(a.loanAmount)}</td>
                  <td style={{ color:'var(--navy-600)' }}>{a.tenureMonths} mo</td>
                  <td style={{ color:'var(--navy-600)' }}>{a.emiAmount ? fmtCurrency(a.emiAmount)+'/mo' : '—'}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem' }}>{fmtDate(a.createdAt)}</td>
                  <td><ChevronRight size={15} style={{ color:'var(--navy-300)' }} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
