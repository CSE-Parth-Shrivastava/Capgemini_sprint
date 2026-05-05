import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Eye, EyeOff, ArrowRight, CheckCircle2 } from 'lucide-react';
import { authApi } from '../../api/services';
import { extractError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';

const PERKS = [
  'Instant CIBIL-style credit score',
  'Track loan status in real-time',
  'Secure document management',
  'AI-powered loan insights',
];

export default function SignupPage() {
  const { login }  = useAuth();
  const navigate   = useNavigate();
  const location   = useLocation();
  const from       = location.state?.from;

  const [form,    setForm]    = useState({ fullName:'', email:'', phone:'', password:'' });
  const [show,    setShow]    = useState(false);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res = await authApi.signup(form);
      const { token, email, role, id } = res.data;
      login(token, { email, role, id });
      if (from && from !== '/login' && from !== '/signup') {
        navigate(from, { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      {/* Left panel */}
      <div className="auth-left">
        <div style={{ position:'relative', zIndex:1, maxWidth:480 }}>
          <div style={{ display:'flex', alignItems:'center', gap:'0.625rem', marginBottom:'3rem' }}>
            <div style={{ width:36, height:36, background:'linear-gradient(135deg,var(--teal-500),var(--teal-700))', borderRadius:'var(--radius)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:800, fontSize:'0.875rem', color:'#fff' }}>FF</div>
            <span style={{ fontWeight:800, fontSize:'1.125rem', color:'#fff', letterSpacing:'-0.02em' }}>Fin<span style={{ color:'var(--teal-400)' }}>Flow</span></span>
          </div>

          <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.5rem', color:'#fff', lineHeight:1.2, marginBottom:'1.25rem' }}>
            Smart lending,<br/><span style={{ color:'var(--teal-400)', fontStyle:'italic' }}>made simple</span>
          </h2>
          <p style={{ color:'rgba(255,255,255,0.55)', fontSize:'1rem', lineHeight:1.75, marginBottom:'2.5rem' }}>
            Join 2.5 lakh+ borrowers who manage their loans digitally with FinFlow.
          </p>

          <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
            {PERKS.map(p => (
              <div key={p} style={{ display:'flex', alignItems:'center', gap:'0.75rem' }}>
                <CheckCircle2 size={18} style={{ color:'var(--teal-400)', flexShrink:0 }} />
                <span style={{ color:'rgba(255,255,255,0.75)', fontSize:'0.9375rem' }}>{p}</span>
              </div>
            ))}
          </div>

          <div style={{ marginTop:'3rem', padding:'1.5rem', background:'rgba(255,255,255,0.04)', borderRadius:'var(--radius-xl)', border:'1.5px solid rgba(255,255,255,0.08)' }}>
            <div style={{ display:'flex', gap:'0.5rem', marginBottom:'0.75rem' }}>
              {[1,2,3,4,5].map(i => <span key={i} style={{ fontSize:13, color:'var(--amber-400)' }}>★</span>)}
            </div>
            <p style={{ color:'rgba(255,255,255,0.7)', fontSize:'0.9rem', fontStyle:'italic', lineHeight:1.7, marginBottom:'0.75rem' }}>
              "Got my home loan approved in 2 days flat. The CIBIL checker told me exactly what to fix. Incredible product."
            </p>
            <div style={{ fontSize:'0.8125rem', color:'rgba(255,255,255,0.4)', fontWeight:500 }}>— Priya S., Mumbai</div>
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
            <h2 style={{ marginBottom:'0.5rem', fontSize:'1.625rem' }}>Create your account</h2>
            <p style={{ color:'var(--navy-500)', fontSize:'0.9375rem' }}>
              Already have one?{' '}
              <Link to="/login" style={{ fontWeight:600, color:'var(--teal-600)' }}>Sign in</Link>
            </p>
          </div>

          {error && <div className="alert alert-error" style={{ marginBottom:'1.25rem' }}>{error}</div>}

          <form onSubmit={handleSubmit} style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
            <div className="form-group">
              <label className="form-label">Full name <span className="required">*</span></label>
              <input className="form-input" type="text" placeholder="Priya Sharma" required autoFocus
                value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
            </div>

            <div className="form-group">
              <label className="form-label">Email address <span className="required">*</span></label>
              <input className="form-input" type="email" placeholder="you@example.com" required
                value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
            </div>

            <div className="form-group">
              <label className="form-label">Mobile number <span className="required">*</span></label>
              <input className="form-input" type="tel" placeholder="10-digit mobile" maxLength={10} required
                value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value.replace(/\D/,'') }))} />
            </div>

            <div className="form-group">
              <label className="form-label">Password <span className="required">*</span></label>
              <div style={{ position:'relative' }}>
                <input className="form-input" type={show ? 'text' : 'password'} placeholder="Min. 8 characters" required minLength={8}
                  style={{ paddingRight:'2.75rem' }}
                  value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
                <button type="button" onClick={() => setShow(s => !s)}
                  style={{ position:'absolute', right:'0.75rem', top:'50%', transform:'translateY(-50%)', background:'none', border:'none', cursor:'pointer', color:'var(--navy-400)', display:'flex', alignItems:'center' }}>
                  {show ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <p style={{ fontSize:'0.75rem', color:'var(--navy-400)', lineHeight:1.6, marginTop:'0.25rem' }}>
              By creating an account you agree to our{' '}
              <a href="#" style={{ color:'var(--teal-600)' }}>Terms of Service</a> and{' '}
              <a href="#" style={{ color:'var(--teal-600)' }}>Privacy Policy</a>.
            </p>

            <button className="btn btn-primary btn-lg" type="submit" disabled={loading} style={{ width:'100%', justifyContent:'center' }}>
              {loading ? (
                <><span className="spinner spinner-sm" style={{ borderTopColor:'#fff' }} /> Creating account…</>
              ) : (
                <>Create Account <ArrowRight size={16} /></>
              )}
            </button>
          </form>

          <div style={{ marginTop:'1.5rem', textAlign:'center' }}>
            <Link to="/" style={{ fontSize:'0.875rem', color:'var(--navy-400)' }}>← Back to Home</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
