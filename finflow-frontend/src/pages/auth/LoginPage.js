import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Eye, EyeOff, ArrowRight, Shield, TrendingUp, Zap } from 'lucide-react';
import { authApi } from '../../api/services';
import { extractError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate  = useNavigate();
  const location  = useLocation();
  const from      = location.state?.from;

  const [form,   setForm]    = useState({ email: '', password: '' });
  const [show,   setShow]    = useState(false);
  const [loading,setLoading] = useState(false);
  const [error,  setError]   = useState('');

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res  = await authApi.login(form);
      const { token, email, role, id } = res.data;
      login(token, { email, role, id });
      // Redirect: intended page → role dashboard → home
      if (from && from !== '/login' && from !== '/signup') {
        navigate(from, { replace: true });
      } else {
        navigate(role === 'ADMIN' ? '/admin/dashboard' : '/dashboard', { replace: true });
      }
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      {/* Left decorative panel */}
      <div className="auth-left">
        <div style={{ position:'relative', zIndex:1, maxWidth:480 }}>
          <div style={{ display:'flex', alignItems:'center', gap:'0.625rem', marginBottom:'3rem' }}>
            <div style={{ width:36, height:36, background:'linear-gradient(135deg,var(--teal-500),var(--teal-700))', borderRadius:'var(--radius)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:800, fontSize:'0.875rem', color:'#fff' }}>FF</div>
            <span style={{ fontWeight:800, fontSize:'1.125rem', color:'#fff', letterSpacing:'-0.02em' }}>Fin<span style={{ color:'var(--teal-400)' }}>Flow</span></span>
          </div>

          <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.5rem', color:'#fff', lineHeight:1.2, marginBottom:'1.25rem' }}>
            Your financial<br/><span style={{ color:'var(--teal-400)', fontStyle:'italic' }}>journey starts here</span>
          </h2>
          <p style={{ color:'rgba(255,255,255,0.55)', fontSize:'1rem', lineHeight:1.75, marginBottom:'3rem' }}>
            Track loan applications, get AI credit scores, and manage documents — all in one place.
          </p>

          <div style={{ display:'flex', flexDirection:'column', gap:'1.25rem' }}>
            {[
              { icon:<Shield size={18}/>,    color:'var(--teal-400)',  title:'Bank-grade Security',    desc:'TLS 1.3 + AES-256 encryption on all data' },
              { icon:<TrendingUp size={18}/>, color:'var(--amber-400)', title:'AI Credit Assessment',   desc:'CIBIL-style scores with actionable insights' },
              { icon:<Zap size={18}/>,        color:'#a78bfa',          title:'Instant Approval Alerts', desc:'Real-time notifications at every step' },
            ].map(({ icon, color, title, desc }) => (
              <div key={title} style={{ display:'flex', gap:'1rem', alignItems:'flex-start' }}>
                <div style={{ width:40, height:40, borderRadius:'var(--radius)', background:'rgba(255,255,255,0.07)', display:'flex', alignItems:'center', justifyContent:'center', color, flexShrink:0 }}>{icon}</div>
                <div>
                  <div style={{ fontWeight:600, color:'#fff', fontSize:'0.9375rem', marginBottom:'0.25rem' }}>{title}</div>
                  <div style={{ color:'rgba(255,255,255,0.45)', fontSize:'0.8125rem' }}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right form */}
      <div className="auth-right">
        <div style={{ flex:1, display:'flex', flexDirection:'column', justifyContent:'center', maxWidth:380, margin:'0 auto', width:'100%' }}>
          <div style={{ marginBottom:'2rem' }}>
            <div style={{ display:'flex', alignItems:'center', gap:'0.625rem', marginBottom:'1.75rem' }}>
              <div style={{ width:32, height:32, background:'linear-gradient(135deg,var(--teal-500),var(--teal-700))', borderRadius:'var(--radius-sm)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:800, fontSize:'0.75rem', color:'#fff' }}>FF</div>
              <span style={{ fontWeight:800, fontSize:'1rem', color:'var(--navy-900)', letterSpacing:'-0.02em' }}>Fin<span style={{ color:'var(--teal-600)' }}>Flow</span></span>
            </div>
            <h2 style={{ marginBottom:'0.5rem', fontSize:'1.625rem' }}>Welcome back</h2>
            <p style={{ color:'var(--navy-500)', fontSize:'0.9375rem' }}>
              Don't have an account?{' '}
              <Link to="/signup" style={{ fontWeight:600, color:'var(--teal-600)' }}>Sign up free</Link>
            </p>
          </div>

          {from && from !== '/' && (
            <div className="alert alert-info" style={{ marginBottom:'1.25rem', fontSize:'0.8125rem' }}>
              Please sign in to continue to your destination.
            </div>
          )}

          {error && (
            <div className="alert alert-error" style={{ marginBottom:'1.25rem' }}>{error}</div>
          )}

          <form onSubmit={handleSubmit} style={{ display:'flex', flexDirection:'column', gap:'1.125rem' }}>
            <div className="form-group">
              <label className="form-label">Email address <span className="required">*</span></label>
              <input className="form-input" type="email" placeholder="you@example.com" required autoFocus
                value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
            </div>

            <div className="form-group">
              <label className="form-label">Password <span className="required">*</span></label>
              <div style={{ position:'relative' }}>
                <input className="form-input" type={show ? 'text' : 'password'} placeholder="Enter your password" required
                  style={{ paddingRight:'2.75rem' }}
                  value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
                <button type="button" onClick={() => setShow(s => !s)}
                  style={{ position:'absolute', right:'0.75rem', top:'50%', transform:'translateY(-50%)', background:'none', border:'none', cursor:'pointer', color:'var(--navy-400)', display:'flex', alignItems:'center' }}>
                  {show ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <button className="btn btn-primary btn-lg" type="submit" disabled={loading} style={{ width:'100%', justifyContent:'center', marginTop:'0.25rem' }}>
              {loading ? (
                <><span className="spinner spinner-sm" style={{ borderTopColor:'#fff' }} /> Signing in…</>
              ) : (
                <>Sign In <ArrowRight size={16} /></>
              )}
            </button>
          </form>

          <div style={{ marginTop:'2rem', padding:'1.25rem', background:'var(--navy-50)', borderRadius:'var(--radius-lg)', border:'1px solid var(--navy-100)' }}>
            <div style={{ fontSize:'0.75rem', fontWeight:700, color:'var(--navy-500)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.75rem' }}>Demo Credentials</div>
            <div style={{ display:'flex', flexDirection:'column', gap:'0.5rem', fontSize:'0.8125rem' }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                <span style={{ color:'var(--navy-600)' }}>Applicant</span>
                <span style={{ fontFamily:'monospace', color:'var(--teal-700)', fontWeight:600 }}>applicant@demo.com</span>
              </div>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                <span style={{ color:'var(--navy-600)' }}>Admin</span>
                <span style={{ fontFamily:'monospace', color:'var(--teal-700)', fontWeight:600 }}>admin@demo.com</span>
              </div>
              <div style={{ color:'var(--navy-400)', fontSize:'0.75rem', marginTop:'0.125rem' }}>Password: <code style={{ fontWeight:600 }}>Demo@1234</code></div>
            </div>
          </div>

          <div style={{ marginTop:'1.5rem', textAlign:'center' }}>
            <Link to="/" style={{ fontSize:'0.875rem', color:'var(--navy-400)' }}>← Back to Home</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
