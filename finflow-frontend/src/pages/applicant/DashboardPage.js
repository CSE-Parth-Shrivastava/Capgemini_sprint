import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  PlusCircle, FileText, CheckCircle2, Clock,
  AlertCircle, TrendingUp, ChevronRight, ArrowRight, Sparkles
} from 'lucide-react';
import { applicationApi } from '../../api/services';
import { PageLoader, StatusBadge, Card, Alert } from '../../components/common';
import { fmtCurrency, fmtDate, getLoanTypeLabel } from '../../utils/helpers';
import { useAuth } from '../../context/AuthContext';

function StatCard({ icon, label, value, color, bg }) {
  return (
    <Card style={{ padding:'1.25rem 1.5rem', display:'flex', alignItems:'center', gap:'1rem', borderTop:`3px solid ${color}` }}>
      <div style={{ width:46, height:46, borderRadius:'var(--radius-lg)', background:bg, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0, color }}>
        {icon}
      </div>
      <div>
        <div style={{ fontSize:'1.875rem', fontWeight:800, lineHeight:1, color:'var(--navy-900)', letterSpacing:'-0.03em' }}>{value}</div>
        <div style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:3, fontWeight:500 }}>{label}</div>
      </div>
    </Card>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [apps, setApps]       = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  useEffect(() => {
    applicationApi.getMyList()
      .then(r => setApps(r.data))
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  const total    = apps.length;
  const approved = apps.filter(a => a.status === 'APPROVED').length;
  const pending  = apps.filter(a => ['SUBMITTED','DOCS_PENDING','DOCS_VERIFIED','UNDER_REVIEW'].includes(a.status)).length;
  const draft    = apps.filter(a => a.status === 'DRAFT').length;
  const recent   = [...apps].sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5);

  return (
    <div>
      {error && <Alert type="error" style={{ marginBottom:'1.5rem' }}>{error}</Alert>}

      {/* Welcome banner */}
      <div style={{
        background:'linear-gradient(135deg, var(--teal-800) 0%, var(--navy-900) 100%)',
        borderRadius:'var(--radius-2xl)', padding:'2rem 2.25rem',
        marginBottom:'1.75rem', color:'#fff',
        display:'flex', alignItems:'center', justifyContent:'space-between',
        flexWrap:'wrap', gap:'1rem', position:'relative', overflow:'hidden'
      }}>
        <div style={{ position:'absolute', width:300, height:300, background:'radial-gradient(circle,rgba(20,184,166,0.15) 0%,transparent 70%)', top:-100, right:80, pointerEvents:'none' }} />
        <div style={{ position:'relative', zIndex:1 }}>
          <div style={{ fontSize:'0.8125rem', color:'rgba(255,255,255,0.55)', fontWeight:600, marginBottom:'0.375rem' }}>
            Welcome back 👋
          </div>
          <h2 style={{ color:'#fff', marginBottom:'0.5rem', fontSize:'1.5rem' }}>
            {user?.email?.split('@')[0] || 'Hello'}
          </h2>
          <p style={{ color:'rgba(255,255,255,0.6)', fontSize:'0.9375rem' }}>
            {total === 0 ? "Start your first loan application today." : `You have ${pending} application${pending !== 1 ? 's' : ''} in progress.`}
          </p>
        </div>
        <button className="btn btn-amber" onClick={() => navigate('/applications/new')} style={{ position:'relative', zIndex:1, fontWeight:700 }}>
          <PlusCircle size={16} /> Apply for Loan
        </button>
      </div>

      {/* Stats */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'1rem', marginBottom:'1.75rem' }}>
        <StatCard icon={<FileText size={20}/>}     label="Total Applications" value={total}    color="var(--teal-600)"  bg="var(--teal-50)" />
        <StatCard icon={<Clock size={20}/>}         label="In Progress"        value={pending}  color="var(--blue-600)"  bg="var(--blue-50)" />
        <StatCard icon={<CheckCircle2 size={20}/>}  label="Approved"           value={approved} color="var(--green-600)" bg="var(--green-50)" />
        <StatCard icon={<AlertCircle size={20}/>}   label="Draft"              value={draft}    color="var(--amber-600)" bg="var(--amber-50)" />
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'1fr 340px', gap:'1.25rem' }}>
        {/* Recent applications */}
        <Card>
          <div style={{ padding:'1.25rem 1.5rem', borderBottom:'1px solid var(--navy-100)', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <div>
              <div style={{ fontWeight:700, fontSize:'1rem', color:'var(--navy-900)' }}>Recent Applications</div>
              <div style={{ fontSize:'0.8125rem', color:'var(--navy-400)', marginTop:2 }}>Your latest loan applications</div>
            </div>
            <Link to="/applications" className="btn btn-secondary btn-sm">
              View all <ChevronRight size={13} />
            </Link>
          </div>

          {recent.length === 0 ? (
            <div style={{ padding:'3.5rem 2rem', textAlign:'center' }}>
              <div style={{ width:56, height:56, background:'var(--navy-100)', borderRadius:'var(--radius-xl)', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 1rem', color:'var(--navy-400)' }}>
                <TrendingUp size={24} />
              </div>
              <div style={{ fontWeight:700, color:'var(--navy-700)', marginBottom:'0.5rem' }}>No applications yet</div>
              <p style={{ fontSize:'0.875rem', color:'var(--navy-400)', marginBottom:'1.25rem' }}>
                Start your first loan application to get going.
              </p>
              <button className="btn btn-primary" onClick={() => navigate('/applications/new')}>
                <PlusCircle size={15} /> Apply Now
              </button>
            </div>
          ) : (
            <div>
              {recent.map((app, idx) => (
                <Link key={app.id} to={`/applications/${app.id}`} style={{ display:'flex', alignItems:'center', gap:'1rem', padding:'0.875rem 1.5rem', borderBottom: idx < recent.length-1 ? '1px solid var(--navy-50)' : 'none', textDecoration:'none', transition:'background var(--transition)' }}
                  onMouseEnter={e => e.currentTarget.style.background='var(--navy-50)'}
                  onMouseLeave={e => e.currentTarget.style.background='transparent'}
                >
                  <div style={{ width:40, height:40, borderRadius:'var(--radius-lg)', background:'var(--teal-50)', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--teal-600)', flexShrink:0, fontSize:'0.75rem', fontWeight:700 }}>
                    #{app.id}
                  </div>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontWeight:600, color:'var(--navy-800)', fontSize:'0.9375rem', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                      {getLoanTypeLabel(app.loanType)}
                    </div>
                    <div style={{ fontSize:'0.8125rem', color:'var(--navy-400)', marginTop:2 }}>
                      {fmtCurrency(app.loanAmount)} · {fmtDate(app.createdAt)}
                    </div>
                  </div>
                  <StatusBadge status={app.status} />
                  <ChevronRight size={14} style={{ color:'var(--navy-300)', flexShrink:0 }} />
                </Link>
              ))}
            </div>
          )}
        </Card>

        {/* Quick actions */}
        <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
          <Card style={{ padding:'1.5rem' }}>
            <div style={{ fontWeight:700, color:'var(--navy-900)', marginBottom:'1rem' }}>Quick Actions</div>
            <div style={{ display:'flex', flexDirection:'column', gap:'0.625rem' }}>
              {[
                { label:'Apply for New Loan',    icon:<PlusCircle size={16}/>,   path:'/applications/new',  color:'var(--teal-600)',  bg:'var(--teal-50)' },
                { label:'View All Applications', icon:<FileText size={16}/>,     path:'/applications',      color:'var(--blue-600)',  bg:'var(--blue-50)' },
                { label:'Upload Documents',      icon:<FileText size={16}/>,     path:'/documents',         color:'var(--amber-600)', bg:'var(--amber-50)' },
                { label:'Check Notifications',   icon:<AlertCircle size={16}/>,  path:'/notifications',     color:'var(--navy-600)',  bg:'var(--navy-100)' },
              ].map(({ label, icon, path, color, bg }) => (
                <button key={label} className="btn btn-secondary" style={{ justifyContent:'flex-start', gap:'0.75rem', padding:'0.75rem 1rem', width:'100%', textAlign:'left' }} onClick={() => navigate(path)}>
                  <div style={{ width:32, height:32, borderRadius:'var(--radius)', background:bg, display:'flex', alignItems:'center', justifyContent:'center', color, flexShrink:0 }}>
                    {icon}
                  </div>
                  <span style={{ flex:1, fontWeight:600, color:'var(--navy-700)' }}>{label}</span>
                  <ArrowRight size={13} style={{ color:'var(--navy-300)' }} />
                </button>
              ))}
            </div>
          </Card>

          {/* CIBIL promo */}
          <Card style={{ padding:'1.5rem', background:'linear-gradient(135deg,var(--teal-50),#fff)', border:'1.5px solid var(--teal-100)' }}>
            <div style={{ display:'flex', alignItems:'center', gap:'0.5rem', fontWeight:700, color:'var(--teal-700)', marginBottom:'0.625rem', fontSize:'0.9375rem' }}>
              <Sparkles size={16} /> AI Credit Score
            </div>
            <p style={{ fontSize:'0.8125rem', color:'var(--teal-600)', lineHeight:1.65, marginBottom:'1rem' }}>
              Get your CIBIL-style score (300–850) with AI-powered insights and improvement tips.
            </p>
            <button className="btn btn-primary btn-sm" style={{ width:'100%', justifyContent:'center' }} onClick={() => navigate('/applications')}>
              Check Score <ArrowRight size={13} />
            </button>
          </Card>
        </div>
      </div>
    </div>
  );
}
