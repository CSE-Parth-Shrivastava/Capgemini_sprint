import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { PlusCircle, ChevronRight, Search } from 'lucide-react';
import { applicationApi } from '../../api/services';
import { PageLoader, Alert, StatusBadge, Card, EmptyState, SectionHeader } from '../../components/common';
import { fmtCurrency, fmtDate, getLoanTypeLabel } from '../../utils/helpers';

export default function ApplicationsListPage() {
  const navigate = useNavigate();
  const [apps,    setApps]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [search,  setSearch]  = useState('');
  const [filter,  setFilter]  = useState('ALL');

  useEffect(() => {
    applicationApi.getMyList()
      .then(r => setApps(r.data))
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false));
  }, []);

  const STATUSES = ['ALL','DRAFT','SUBMITTED','DOCS_PENDING','DOCS_VERIFIED','APPROVED','REJECTED'];

  const filtered = apps.filter(a => {
    const matchStatus = filter === 'ALL' || a.status === filter;
    const q = search.toLowerCase();
    const matchSearch = !q || getLoanTypeLabel(a.loanType).toLowerCase().includes(q) || String(a.id).includes(q) || a.purpose?.toLowerCase().includes(q);
    return matchStatus && matchSearch;
  });

  if (loading) return <PageLoader />;

  return (
    <div>
      <SectionHeader
        title="My Applications"
        subtitle={`${apps.length} total application${apps.length !== 1 ? 's' : ''}`}
        action={
          <button className="btn btn-primary" onClick={() => navigate('/applications/new')}>
            <PlusCircle size={15} /> New Application
          </button>
        }
      />

      {error && <Alert type="error" style={{ marginBottom:'1rem' }}>{error}</Alert>}

      {/* Filters */}
      <div style={{ display:'flex', gap:'0.75rem', marginBottom:'1.25rem', flexWrap:'wrap', alignItems:'center' }}>
        <div style={{ position:'relative', flex:1, minWidth:200 }}>
          <Search size={15} style={{ position:'absolute', left:'0.75rem', top:'50%', transform:'translateY(-50%)', color:'var(--navy-400)' }} />
          <input className="form-input" style={{ paddingLeft:'2.25rem' }} placeholder="Search by loan type, purpose, ID…"
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div style={{ display:'flex', gap:'0.375rem', flexWrap:'wrap' }}>
          {STATUSES.map(s => (
            <button key={s} className={`btn btn-sm ${filter === s ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setFilter(s)} style={{ textTransform: s === 'ALL' ? undefined : undefined }}>
              {s === 'ALL' ? 'All' : s.replace(/_/g,' ')}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <Card>
          <EmptyState
            icon="📄"
            title={search || filter !== 'ALL' ? 'No applications match your filters' : 'No applications yet'}
            description={search || filter !== 'ALL' ? 'Try adjusting your search or filter.' : 'Apply for a loan to get started.'}
            action={!search && filter === 'ALL' && (
              <button className="btn btn-primary" onClick={() => navigate('/applications/new')}>
                <PlusCircle size={15} /> Apply for Loan
              </button>
            )}
          />
        </Card>
      ) : (
        <Card>
          <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Loan Type</th>
                  <th>Amount</th>
                  <th>Tenure</th>
                  <th>EMI</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(app => (
                  <tr key={app.id} style={{ cursor:'pointer' }} onClick={() => navigate(`/applications/${app.id}`)}>
                    <td style={{ fontWeight:600, color:'var(--navy-400)', fontSize:'0.8125rem' }}>#{app.id}</td>
                    <td style={{ fontWeight:500 }}>{getLoanTypeLabel(app.loanType)}</td>
                    <td style={{ fontWeight:600 }}>{fmtCurrency(app.loanAmount)}</td>
                    <td style={{ color:'var(--navy-600)' }}>{app.tenureMonths} mo</td>
                    <td style={{ color:'var(--navy-600)' }}>{app.emiAmount ? fmtCurrency(app.emiAmount)+'/mo' : '—'}</td>
                    <td><StatusBadge status={app.status} /></td>
                    <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem' }}>{fmtDate(app.createdAt)}</td>
                    <td><ChevronRight size={16} style={{ color:'var(--navy-300)' }} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
