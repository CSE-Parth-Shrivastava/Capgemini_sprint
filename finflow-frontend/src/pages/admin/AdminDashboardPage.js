import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardList, CheckCircle2, XCircle, Clock, BarChart3, ChevronRight, Users, TrendingUp, FileCheck } from 'lucide-react';
import { applicationApi, adminApi, documentApi, authApi } from '../../api/services';
import { PageLoader, Alert, StatusBadge, Card, SectionHeader } from '../../components/common';
import { fmtCurrency, fmtDate, getLoanTypeLabel } from '../../utils/helpers';

function StatCard({ icon, label, value, sub, color, bg }) {
  return (
    <Card style={{ padding:'1.25rem 1.5rem', borderLeft:`3px solid ${color}` }}>
      <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between' }}>
        <div>
          <div style={{ fontSize:'1.875rem', fontWeight:800, lineHeight:1, color:'var(--navy-900)', letterSpacing:'-0.03em' }}>{value}</div>
          <div style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:4, fontWeight:500 }}>{label}</div>
          {sub && <div style={{ fontSize:'0.75rem', color, marginTop:4, fontWeight:600 }}>{sub}</div>}
        </div>
        <div style={{ width:44, height:44, borderRadius:'var(--radius-lg)', background:bg || `${color}15`, display:'flex', alignItems:'center', justifyContent:'center', color }}>
          {icon}
        </div>
      </div>
    </Card>
  );
}

export default function AdminDashboardPage() {
  const navigate = useNavigate();
  const [apps,    setApps]    = useState([]);
  const [pending, setPending] = useState([]);
  const [reports, setReports] = useState(null);
  const [users,   setUsers]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    Promise.all([
      applicationApi.getAll(),
      documentApi.getPending(),
      adminApi.getReports(),
      authApi.getUsers(),
    ]).then(([appR, docR, repR, usrR]) => {
      setApps(appR.data);
      setPending(docR.data);
      setReports(repR.data);
      setUsers(usrR.data);
    }).catch(() => setError('Failed to load dashboard data.'))
    .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  const docsVerified = apps.filter(a => a.status === 'DOCS_VERIFIED').length;
  const approved     = apps.filter(a => a.status === 'APPROVED').length;
  const rejected     = apps.filter(a => a.status === 'REJECTED').length;
  const recent       = [...apps].sort((a,b) => new Date(b.createdAt)-new Date(a.createdAt)).slice(0, 8);

  return (
    <div>
      {error && <Alert type="error" style={{ marginBottom:'1.25rem' }}>{error}</Alert>}

      {/* Admin welcome banner */}
      <div style={{
        background:'linear-gradient(135deg,var(--navy-800) 0%,var(--navy-900) 100%)',
        borderRadius:'var(--radius-2xl)', padding:'1.75rem 2rem',
        marginBottom:'1.5rem', display:'flex', alignItems:'center', justifyContent:'space-between',
        flexWrap:'wrap', gap:'1rem', position:'relative', overflow:'hidden'
      }}>
        <div style={{ position:'absolute', width:250, height:250, background:'radial-gradient(circle,rgba(20,184,166,0.1) 0%,transparent 70%)', top:-80, right:60, pointerEvents:'none' }} />
        <div style={{ position:'relative', zIndex:1 }}>
          <div style={{ fontSize:'0.8125rem', color:'rgba(255,255,255,0.5)', fontWeight:600, marginBottom:'0.25rem' }}>Administration Panel</div>
          <h2 style={{ color:'#fff', marginBottom:'0.375rem', fontSize:'1.375rem' }}>Admin Dashboard</h2>
          <p style={{ color:'rgba(255,255,255,0.5)', fontSize:'0.875rem' }}>
            Overview of all loan applications and pending tasks
          </p>
        </div>
        <div style={{ display:'flex', gap:'0.75rem', position:'relative', zIndex:1 }}>
          <button className="btn btn-sm" style={{ background:'rgba(255,255,255,0.08)', color:'rgba(255,255,255,0.8)', borderColor:'rgba(255,255,255,0.12)' }} onClick={() => navigate('/admin/applications')}>
            <ClipboardList size={14} /> Applications
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => navigate('/admin/documents')}>
            <FileCheck size={14} /> Review Docs
          </button>
        </div>
      </div>

      {/* Stats grid */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(190px,1fr))', gap:'1rem', marginBottom:'1.5rem' }}>
        <StatCard icon={<ClipboardList size={20}/>} label="Total Applications" value={apps.length}    color="var(--teal-600)"  />
        <StatCard icon={<Clock size={20}/>}         label="Ready for Decision" value={docsVerified}   color="var(--blue-600)"  sub="Docs verified" />
        <StatCard icon={<CheckCircle2 size={20}/>}  label="Approved"           value={approved}       color="var(--green-600)" sub={reports?.approvalRate} />
        <StatCard icon={<XCircle size={20}/>}       label="Rejected"           value={rejected}       color="var(--red-500)"   />
        <StatCard icon={<TrendingUp size={20}/>}    label="Pending Docs"       value={pending.length} color="var(--amber-600)" sub="Awaiting review" />
        <StatCard icon={<Users size={20}/>}         label="Total Users"        value={users.length}   color="var(--navy-600)"  />
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'1fr 340px', gap:'1.25rem' }}>
        {/* Applications table */}
        <Card>
          <div style={{ padding:'1.125rem 1.5rem', borderBottom:'1px solid var(--navy-100)', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <div>
              <div style={{ fontWeight:700, color:'var(--navy-900)' }}>Recent Applications</div>
              <div style={{ fontSize:'0.8125rem', color:'var(--navy-400)', marginTop:2 }}>Latest submitted applications</div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/admin/applications')}>
              View all <ChevronRight size={13}/>
            </button>
          </div>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr><th>ID</th><th>Loan Type</th><th>Amount</th><th>Status</th><th>Date</th><th></th></tr>
              </thead>
              <tbody>
                {recent.map(a => (
                  <tr key={a.id} style={{ cursor:'pointer' }} onClick={() => navigate(`/admin/applications/${a.id}`)}>
                    <td style={{ fontWeight:700, color:'var(--teal-600)', fontSize:'0.8125rem' }}>#{a.id}</td>
                    <td style={{ fontWeight:600 }}>{getLoanTypeLabel(a.loanType)}</td>
                    <td style={{ fontWeight:600 }}>{fmtCurrency(a.loanAmount)}</td>
                    <td><StatusBadge status={a.status}/></td>
                    <td style={{ color:'var(--navy-400)', fontSize:'0.8125rem' }}>{fmtDate(a.createdAt)}</td>
                    <td><ChevronRight size={14} style={{ color:'var(--navy-300)' }}/></td>
                  </tr>
                ))}
                {recent.length === 0 && (
                  <tr><td colSpan={6} style={{ textAlign:'center', padding:'3rem', color:'var(--navy-400)' }}>No applications yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Pending docs */}
        <Card>
          <div style={{ padding:'1.125rem 1.25rem', borderBottom:'1px solid var(--navy-100)', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <div style={{ fontWeight:700, color:'var(--navy-900)' }}>Pending Documents</div>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/admin/documents')}>
              View all <ChevronRight size={13}/>
            </button>
          </div>
          <div style={{ maxHeight:380, overflowY:'auto' }}>
            {pending.length === 0 ? (
              <div style={{ padding:'2.5rem 1.5rem', textAlign:'center', color:'var(--navy-400)' }}>
                <CheckCircle2 size={28} style={{ marginBottom:8, color:'var(--green-500)' }} />
                <div style={{ fontWeight:600, color:'var(--navy-600)', fontSize:'0.9rem' }}>All caught up!</div>
                <div style={{ fontSize:'0.8125rem', marginTop:4 }}>No documents pending review</div>
              </div>
            ) : pending.slice(0,10).map(doc => (
              <div key={doc.id} style={{ padding:'0.875rem 1.25rem', borderBottom:'1px solid var(--navy-50)', cursor:'pointer', transition:'background var(--transition)' }}
                onClick={() => navigate('/admin/documents')}
                onMouseEnter={e => e.currentTarget.style.background='var(--navy-50)'}
                onMouseLeave={e => e.currentTarget.style.background='transparent'}
              >
                <div style={{ fontSize:'0.875rem', fontWeight:600, color:'var(--navy-800)' }}>
                  {doc.documentType?.replace(/_/g,' ')}
                </div>
                <div style={{ fontSize:'0.75rem', color:'var(--navy-400)', marginTop:3 }}>
                  App #{doc.applicationId} · {doc.fileName}
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
