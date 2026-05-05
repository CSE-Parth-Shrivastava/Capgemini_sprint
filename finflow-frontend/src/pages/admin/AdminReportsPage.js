import React, { useState, useEffect } from 'react';
import { BarChart3, CheckCircle2, XCircle, ClipboardList, TrendingUp } from 'lucide-react';
import { adminApi } from '../../api/services';
import { PageLoader, Alert, Card, SectionHeader } from '../../components/common';

function ReportCard({ icon, label, value, color, sub }) {
  return (
    <Card style={{ padding:'1.5rem' }}>
      <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:'1rem' }}>
        <div>
          <div style={{ fontSize:'2.25rem', fontWeight:800, lineHeight:1, color:'var(--navy-900)' }}>{value ?? '—'}</div>
          <div style={{ fontSize:'0.875rem', color:'var(--navy-500)', marginTop:6 }}>{label}</div>
          {sub && <div style={{ fontSize:'0.8125rem', fontWeight:600, color, marginTop:4 }}>{sub}</div>}
        </div>
        <div style={{ width:48, height:48, borderRadius:'var(--radius-xl)', background:`${color}15`, display:'flex', alignItems:'center', justifyContent:'center', color }}>
          {icon}
        </div>
      </div>
    </Card>
  );
}

export default function AdminReportsPage() {
  const [reports, setReports] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    adminApi.getReports()
      .then(r => setReports(r.data))
      .catch(() => setError('Failed to load reports.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  return (
    <div style={{ maxWidth: 900 }}>
      <SectionHeader title="Reports & Analytics" subtitle="Summary of all loan decisions made on FinFlow" />

      {error && <Alert type="error" style={{ marginBottom:'1rem' }}>{error}</Alert>}

      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit, minmax(200px, 1fr))', gap:'1rem', marginBottom:'1.75rem' }}>
        <ReportCard icon={<ClipboardList size={22}/>} label="Total Decisions"   value={reports?.totalDecisions} color="var(--teal-600)" />
        <ReportCard icon={<CheckCircle2  size={22}/>} label="Approved"          value={reports?.approved}       color="var(--green-600)" />
        <ReportCard icon={<XCircle       size={22}/>} label="Rejected"          value={reports?.rejected}       color="var(--red-500)" />
        <ReportCard icon={<TrendingUp    size={22}/>} label="Approval Rate"     value={reports?.approvalRate}   color="var(--blue-500)"
          sub={reports?.approvalRate ? "of all decided applications" : undefined} />
      </div>

      {/* Visual bar */}
      {reports?.totalDecisions > 0 && (
        <Card style={{ padding:'1.5rem' }}>
          <div style={{ fontWeight:600, marginBottom:'1.25rem', display:'flex', alignItems:'center', gap:'0.5rem' }}>
            <BarChart3 size={18} style={{ color:'var(--teal-600)' }} />
            Decision Breakdown
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
            {[
              { label:'Approved', value: reports.approved, total: reports.totalDecisions, color:'var(--green-500)' },
              { label:'Rejected', value: reports.rejected, total: reports.totalDecisions, color:'var(--red-500)' },
            ].map(row => (
              <div key={row.label}>
                <div style={{ display:'flex', justifyContent:'space-between', fontSize:'0.875rem', marginBottom:'0.375rem' }}>
                  <span style={{ fontWeight:500 }}>{row.label}</span>
                  <span style={{ color:'var(--navy-500)' }}>{row.value} / {row.total}</span>
                </div>
                <div style={{ height:10, background:'var(--navy-100)', borderRadius:99, overflow:'hidden' }}>
                  <div style={{ height:'100%', width:`${row.total > 0 ? (row.value/row.total)*100 : 0}%`, background:row.color, borderRadius:99, transition:'width 0.6s ease' }} />
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {reports?.totalDecisions === 0 && (
        <Card>
          <div className="empty-state">
            <BarChart3 size={40} style={{ color:'var(--navy-300)' }} />
            <div className="empty-state-title">No decisions yet</div>
            <p style={{ fontSize:'0.875rem' }}>Reports will appear here once decisions are made on applications.</p>
          </div>
        </Card>
      )}
    </div>
  );
}
