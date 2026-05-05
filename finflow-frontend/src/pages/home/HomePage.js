import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Shield, Zap, TrendingUp, ChevronDown, ChevronRight,
  CheckCircle2, Star, ArrowRight, Clock, Award,
  Home, Car, GraduationCap, Briefcase, Gem, CreditCard,
  Phone, Building2, Users, BarChart3
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { calcEmi } from '../../utils/helpers';

function PublicNav() {
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();
  const dashPath = isAdmin ? '/admin/dashboard' : '/dashboard';
  return (
    <nav className="pub-nav">
      <div className="pub-nav-inner">
        <Link to="/" className="pub-nav-logo">
          <div className="pub-nav-logo-icon">FF</div>
          <div className="pub-nav-logo-text">Fin<span>Flow</span></div>
        </Link>
        <div className="pub-nav-links">
          <a href="#rates"      className="pub-nav-link">Loan Rates</a>
          <a href="#calculator" className="pub-nav-link">EMI Calculator</a>
          <a href="#how"        className="pub-nav-link">How It Works</a>
          <a href="#faq"        className="pub-nav-link">FAQs</a>
        </div>
        <div className="pub-nav-actions">
          {user ? (
            <>
              <button className="btn btn-ghost btn-sm" style={{ color: 'rgba(255,255,255,0.7)' }} onClick={() => navigate(dashPath)}>Dashboard</button>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/applications/new')}>Apply Now</button>
            </>
          ) : (
            <>
              <Link to="/login"  className="btn btn-ghost btn-sm" style={{ color: 'rgba(255,255,255,0.7)' }}>Sign In</Link>
              <Link to="/signup" className="btn btn-primary btn-sm">Get Started</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}

const LOAN_PRODUCTS = [
  { icon: <Home size={22}/>,         label:'Home Loan',      rate:'8.50%',  maxAmt:'₹5 Cr',  tenure:'Up to 30 yrs', color:'#0d9488', bg:'#f0fdfa', type:'HOME' },
  { icon: <Car size={22}/>,          label:'Car Loan',       rate:'9.25%',  maxAmt:'₹50 L',  tenure:'Up to 7 yrs',  color:'#2563eb', bg:'#eff6ff', type:'CAR' },
  { icon: <CreditCard size={22}/>,   label:'Personal Loan',  rate:'12.99%', maxAmt:'₹25 L',  tenure:'Up to 5 yrs',  color:'#7c3aed', bg:'#f5f3ff', type:'PERSONAL' },
  { icon: <GraduationCap size={22}/>,label:'Education Loan', rate:'9.99%',  maxAmt:'₹1 Cr',  tenure:'Up to 10 yrs', color:'#d97706', bg:'#fffbeb', type:'EDUCATION' },
  { icon: <Briefcase size={22}/>,    label:'Business Loan',  rate:'13.50%', maxAmt:'₹2 Cr',  tenure:'Up to 7 yrs',  color:'#0891b2', bg:'#ecfeff', type:'BUSINESS' },
  { icon: <Gem size={22}/>,          label:'Gold Loan',      rate:'9.50%',  maxAmt:'₹50 L',  tenure:'Up to 3 yrs',  color:'#b45309', bg:'#fef3c7', type:'GOLD' },
];

const FAQS = [
  { q:'What is the minimum credit score required?', a:'We typically require a CIBIL score of 650 or above for most loan products. Our AI-powered assessment considers multiple factors beyond just the score — income stability, employment history, and debt-to-income ratio. Applicants with lower scores may still qualify under specific conditions.' },
  { q:'How long does loan approval take?', a:'In-principle approval can arrive within minutes. Full disbursement after document verification typically takes 24–72 hours for personal loans and 3–7 working days for home and business loans.' },
  { q:'What documents do I need to apply?', a:'You will need: Identity Proof (Aadhaar/Passport/PAN), Income Proof (3 months salary slips / ITR), Address Proof (utility bill / rent agreement), and Bank Statement (last 6 months). Some loan types may require additional documents.' },
  { q:'Can I prepay my loan?', a:'Yes. FinFlow allows partial and full prepayment on all products after the lock-in period (6–12 months). Zero prepayment charges on floating-rate home loans; 2–4% on fixed-rate products.' },
  { q:'What is the CIBIL score feature on FinFlow?', a:"FinFlow's built-in credit assessment generates a CIBIL-style score (300–850) based on income, employment, existing obligations, loan amount, document status, and credit history — with AI-powered improvement tips." },
  { q:'Is my data safe with FinFlow?', a:'Absolutely. All data is encrypted in transit (TLS 1.3) and at rest (AES-256). We follow RBI data localisation guidelines. Documents are stored in isolated, access-controlled storage and never shared without consent.' },
];

const TESTIMONIALS = [
  { name:'Priya Sharma',  role:'Home Loan Customer',     text:'Got my home loan approved in 2 days. The CIBIL score feature showed me exactly what to improve. Brilliant experience.', rating:5, city:'Mumbai' },
  { name:'Rajesh Gupta',  role:'Business Loan Customer', text:'The AI-driven process understood my business financials better than any bank ever did. 10/10 would recommend.',          rating:5, city:'Delhi' },
  { name:'Ananya Iyer',   role:'Education Loan Customer',text:'Seamless document upload and real-time status updates. Knowing my application status 24/7 was incredibly reassuring.',   rating:5, city:'Bengaluru' },
];

function FaqItem({ q, a }) {
  const [open, setOpen] = useState(false);
  return (
    <div className={`faq-item ${open ? 'open' : ''}`}>
      <div className="faq-question" onClick={() => setOpen(o => !o)}>
        <span>{q}</span>
        <ChevronDown size={18} style={{ flexShrink:0, transition:'transform 0.25s', transform: open ? 'rotate(180deg)':'none', color:'var(--teal-600)' }} />
      </div>
      <div className="faq-answer">{a}</div>
    </div>
  );
}

function EmiCalculator() {
  const [amount, setAmount] = useState(500000);
  const [rate,   setRate]   = useState(10.5);
  const [tenure, setTenure] = useState(36);
  const emi      = calcEmi(amount, rate, tenure);
  const total    = emi * tenure;
  const interest = total - amount;
  const fmt = v => new Intl.NumberFormat('en-IN', { style:'currency', currency:'INR', maximumFractionDigits:0 }).format(v);

  return (
    <div className="emi-calc" id="calculator">
      <h3 style={{ color:'#fff', marginBottom:'0.375rem', fontFamily:'var(--ff-display)', fontSize:'1.5rem' }}>EMI Calculator</h3>
      <p style={{ color:'rgba(255,255,255,0.5)', fontSize:'0.875rem', marginBottom:'2rem' }}>Estimate your monthly instalments instantly</p>
      <div style={{ display:'grid', gap:'1.75rem' }}>
        {[
          { label:'Loan Amount', min:50000,  max:10000000, step:50000,  value:amount, set:setAmount, fmt:v => fmt(v),          lo:'₹50K',  hi:'₹1 Cr',  pct:((amount-50000)/9950000)*100 },
          { label:'Interest Rate (p.a.)', min:6, max:24,  step:0.1,    value:rate,   set:setRate,   fmt:v => v.toFixed(1)+'%', lo:'6%',    hi:'24%',    pct:((rate-6)/18)*100 },
          { label:'Tenure', min:3, max:360, step:3,                    value:tenure, set:setTenure, fmt:v => v+' months',      lo:'3 mo',  hi:'360 mo', pct:((tenure-3)/357)*100 },
        ].map(({ label, min, max, step, value, set, fmt:f, lo, hi, pct }) => (
          <div key={label}>
            <div style={{ display:'flex', justifyContent:'space-between', marginBottom:'0.75rem' }}>
              <label style={{ fontSize:'0.75rem', fontWeight:700, color:'rgba(255,255,255,0.5)', textTransform:'uppercase', letterSpacing:'0.06em' }}>{label}</label>
              <span style={{ fontWeight:800, fontSize:'1.0625rem', color:'var(--teal-400)' }}>{f(value)}</span>
            </div>
            <input type="range" min={min} max={max} step={step} value={value}
              onChange={e => set(+e.target.value)}
              style={{ background:`linear-gradient(to right,var(--teal-500) ${pct}%,rgba(255,255,255,0.12) ${pct}%)` }} />
            <div style={{ display:'flex', justifyContent:'space-between', fontSize:'0.7rem', color:'rgba(255,255,255,0.25)', marginTop:'0.25rem' }}>
              <span>{lo}</span><span>{hi}</span>
            </div>
          </div>
        ))}
      </div>
      <div style={{ marginTop:'2rem', padding:'1.5rem', background:'rgba(255,255,255,0.05)', borderRadius:'var(--radius-xl)', border:'1.5px solid rgba(255,255,255,0.08)' }}>
        <div style={{ textAlign:'center', marginBottom:'1.25rem' }}>
          <div style={{ fontSize:'0.75rem', color:'rgba(255,255,255,0.4)', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.08em', marginBottom:'0.25rem' }}>Monthly EMI</div>
          <div style={{ fontSize:'2.25rem', fontWeight:800, color:'var(--teal-400)', letterSpacing:'-0.03em' }}>{fmt(emi)}</div>
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'0.75rem' }}>
          {[
            { label:'Principal',     value:fmt(amount),   color:'var(--teal-300)' },
            { label:'Total Interest',value:fmt(interest), color:'var(--amber-400)' },
            { label:'Total Payment', value:fmt(total),    color:'#fff' },
            { label:'Interest %',    value:`${((interest/total)*100).toFixed(1)}%`, color:'rgba(255,255,255,0.55)' },
          ].map(({ label, value, color }) => (
            <div key={label} style={{ padding:'0.75rem', background:'rgba(255,255,255,0.04)', borderRadius:'var(--radius)', border:'1px solid rgba(255,255,255,0.06)' }}>
              <div style={{ fontSize:'0.65rem', color:'rgba(255,255,255,0.35)', fontWeight:700, textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.125rem' }}>{label}</div>
              <div style={{ fontWeight:800, fontSize:'0.875rem', color }}>{value}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function HomePage() {
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleApply = () => {
    if (!user) { navigate('/login', { state: { from: '/applications/new' } }); return; }
    if (isAdmin) { navigate('/admin/dashboard'); return; }
    navigate('/applications/new');
  };

  return (
    <div style={{ background:'var(--navy-50)' }}>
      <PublicNav />

      {/* Hero */}
      <section className="hero">
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 1.5rem', position:'relative', zIndex:1 }}>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 460px', gap:'4rem', alignItems:'center' }}>
            <div>
              <div style={{ display:'inline-flex', alignItems:'center', gap:'0.5rem', background:'rgba(20,184,166,0.12)', border:'1px solid rgba(20,184,166,0.25)', borderRadius:999, padding:'0.375rem 1rem', marginBottom:'1.75rem' }}>
                <Zap size={13} style={{ color:'var(--teal-400)' }} />
                <span style={{ fontSize:'0.8125rem', fontWeight:600, color:'var(--teal-300)' }}>AI-Powered Loan Assessment</span>
              </div>
              <h1 style={{ fontFamily:'var(--ff-display)', fontSize:'3.25rem', color:'#fff', lineHeight:1.1, marginBottom:'1.25rem' }}>
                Smart Loans for<br/><span style={{ color:'var(--teal-400)', fontStyle:'italic' }}>Real People</span>
              </h1>
              <p style={{ fontSize:'1.0625rem', color:'rgba(255,255,255,0.6)', lineHeight:1.75, maxWidth:480, marginBottom:'2.25rem' }}>
                Apply in minutes. AI-backed CIBIL scores. Track every step. FinFlow brings transparency and speed to loan approvals.
              </p>
              <div style={{ display:'flex', gap:'0.875rem', flexWrap:'wrap', marginBottom:'2.25rem' }}>
                <button className="btn btn-amber btn-xl" onClick={handleApply}>
                  Apply for a Loan <ArrowRight size={18} />
                </button>
                {user ? (
                  <button className="btn btn-xl" style={{ background:'rgba(255,255,255,0.08)', color:'#fff', borderColor:'rgba(255,255,255,0.15)' }} onClick={() => navigate(isAdmin ? '/admin/dashboard' : '/dashboard')}>
                    My Dashboard
                  </button>
                ) : (
                  <Link to="/login" className="btn btn-xl" style={{ background:'rgba(255,255,255,0.08)', color:'#fff', borderColor:'rgba(255,255,255,0.15)' }}>
                    Sign In
                  </Link>
                )}
              </div>
              <div style={{ display:'flex', gap:'1.75rem', flexWrap:'wrap' }}>
                {[{icon:<Shield size={13}/>, text:'RBI Compliant'},{icon:<Zap size={13}/>, text:'Instant Approval'},{icon:<Award size={13}/>, text:'Zero Hidden Fees'}].map(({icon,text}) => (
                  <div key={text} style={{ display:'flex', alignItems:'center', gap:'0.375rem', color:'rgba(255,255,255,0.45)', fontSize:'0.875rem' }}>
                    <span style={{ color:'var(--teal-400)' }}>{icon}</span>{text}
                  </div>
                ))}
              </div>
            </div>
            <EmiCalculator />
          </div>
        </div>
      </section>

      {/* Stats strip */}
      <div className="stat-strip">
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 1.5rem', display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:'2rem' }}>
          {[
            {icon:<Users size={18}/>,     value:'2,50,000+', label:'Happy Customers'},
            {icon:<BarChart3 size={18}/>, value:'₹8,500 Cr', label:'Loans Disbursed'},
            {icon:<Clock size={18}/>,     value:'< 2 Hrs',   label:'Avg. Approval Time'},
            {icon:<Building2 size={18}/>, value:'98.6%',     label:'Customer Satisfaction'},
          ].map(({icon,value,label}) => (
            <div key={label} style={{ textAlign:'center', color:'#fff' }}>
              <div style={{ color:'rgba(255,255,255,0.6)', marginBottom:'0.375rem' }}>{icon}</div>
              <div style={{ fontSize:'1.5rem', fontWeight:800, letterSpacing:'-0.03em', marginBottom:'0.25rem' }}>{value}</div>
              <div style={{ fontSize:'0.8125rem', color:'rgba(255,255,255,0.6)', fontWeight:500 }}>{label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Loan Rates */}
      <section id="rates" style={{ padding:'5rem 0', background:'#fff' }}>
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 1.5rem' }}>
          <div style={{ textAlign:'center', marginBottom:'3rem' }}>
            <div style={{ display:'inline-block', background:'var(--teal-50)', color:'var(--teal-700)', fontSize:'0.8125rem', fontWeight:700, padding:'0.375rem 1rem', borderRadius:999, marginBottom:'1rem', border:'1px solid var(--teal-100)' }}>Loan Products</div>
            <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.25rem', marginBottom:'0.75rem' }}>
              Competitive Rates, <span style={{ color:'var(--teal-600)' }}>Zero Surprises</span>
            </h2>
            <p style={{ color:'var(--navy-500)', maxWidth:520, margin:'0 auto', fontSize:'1rem' }}>
              Transparent pricing. No hidden processing fees. Final rate depends on your credit profile.
            </p>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(280px,1fr))', gap:'1.25rem' }}>
            {LOAN_PRODUCTS.map(({ icon, label, rate, maxAmt, tenure, color, bg, type }) => (
              <div key={type} className="rate-card">
                <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', marginBottom:'1.25rem' }}>
                  <div style={{ width:48, height:48, borderRadius:'var(--radius-lg)', background:bg, display:'flex', alignItems:'center', justifyContent:'center', color }}>
                    {icon}
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:'1.5rem', fontWeight:800, color, letterSpacing:'-0.02em' }}>{rate}</div>
                    <div style={{ fontSize:'0.6875rem', color:'var(--navy-400)', fontWeight:600 }}>per annum</div>
                  </div>
                </div>
                <h4 style={{ marginBottom:'0.875rem' }}>{label}</h4>
                <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'0.5rem', marginBottom:'1.25rem' }}>
                  {[{label:'Max Amount',value:maxAmt},{label:'Tenure',value:tenure}].map(({label:l,value}) => (
                    <div key={l} style={{ padding:'0.625rem 0.75rem', background:'var(--navy-50)', borderRadius:'var(--radius)', border:'1px solid var(--navy-100)' }}>
                      <div style={{ fontSize:'0.6875rem', color:'var(--navy-400)', fontWeight:600, marginBottom:'0.125rem' }}>{l}</div>
                      <div style={{ fontWeight:700, fontSize:'0.875rem', color:'var(--navy-800)' }}>{value}</div>
                    </div>
                  ))}
                </div>
                <button className="btn btn-secondary" style={{ width:'100%', justifyContent:'center' }} onClick={handleApply}>
                  Apply Now <ChevronRight size={14} />
                </button>
              </div>
            ))}
          </div>
          <p style={{ textAlign:'center', color:'var(--navy-400)', fontSize:'0.8125rem', marginTop:'1.5rem' }}>
            * Rates effective May 2025. Subject to change. Final rate basis credit assessment.
          </p>
        </div>
      </section>

      {/* How It Works */}
      <section id="how" style={{ padding:'5rem 0', background:'var(--navy-50)' }}>
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 1.5rem' }}>
          <div style={{ textAlign:'center', marginBottom:'3rem' }}>
            <div style={{ display:'inline-block', background:'var(--amber-50)', color:'var(--amber-600)', fontSize:'0.8125rem', fontWeight:700, padding:'0.375rem 1rem', borderRadius:999, marginBottom:'1rem', border:'1px solid var(--amber-100)' }}>Process</div>
            <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.25rem', marginBottom:'0.75rem' }}>
              Loan in <span style={{ color:'var(--teal-600)', fontStyle:'italic' }}>4 Simple Steps</span>
            </h2>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(220px,1fr))', gap:'1.25rem' }}>
            {[
              {step:'01',title:'Create Account',   desc:'Sign up in under a minute. No paperwork, no branch visit.',         icon:<Shield size={22}/>,      color:'var(--teal-600)'},
              {step:'02',title:'Fill Application', desc:'Answer a few questions about your income and loan requirement.',     icon:<CreditCard size={22}/>,  color:'var(--blue-600)'},
              {step:'03',title:'AI Credit Check',  desc:'Get your CIBIL-style score instantly with AI-powered insights.',    icon:<TrendingUp size={22}/>,   color:'var(--amber-600)'},
              {step:'04',title:'Get Disbursed',    desc:'Upload documents, get approval, and receive funds directly.',       icon:<CheckCircle2 size={22}/>, color:'var(--green-600)'},
            ].map(({ step, title, desc, icon, color }) => (
              <div key={step} className="step-pill" style={{ flexDirection:'column', alignItems:'flex-start', gap:'0.875rem' }}>
                <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', width:'100%' }}>
                  <div style={{ width:44, height:44, borderRadius:'var(--radius-lg)', background:`${color}12`, display:'flex', alignItems:'center', justifyContent:'center', color, flexShrink:0 }}>{icon}</div>
                  <div style={{ fontSize:'1.5rem', fontWeight:800, color:'var(--navy-200)', letterSpacing:'-0.02em', marginLeft:'auto' }}>{step}</div>
                </div>
                <div>
                  <div style={{ fontWeight:700, marginBottom:'0.375rem', color:'var(--navy-800)' }}>{title}</div>
                  <div style={{ fontSize:'0.875rem', color:'var(--navy-500)', lineHeight:1.65 }}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
          <div style={{ textAlign:'center', marginTop:'2.5rem' }}>
            <button className="btn btn-primary btn-lg" onClick={handleApply}>
              Start Your Application <ArrowRight size={16} />
            </button>
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section style={{ padding:'5rem 0', background:'var(--navy-900)' }}>
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 1.5rem' }}>
          <div style={{ textAlign:'center', marginBottom:'3rem' }}>
            <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.25rem', color:'#fff' }}>
              Trusted by <span style={{ color:'var(--teal-400)', fontStyle:'italic' }}>2.5 Lakh+</span> Customers
            </h2>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(300px,1fr))', gap:'1.25rem' }}>
            {TESTIMONIALS.map(({ name, role, text, rating, city }) => (
              <div key={name} className="testimonial-card">
                <div style={{ display:'flex', gap:'0.25rem', marginBottom:'1rem' }}>
                  {Array.from({length:rating}).map((_,i) => <Star key={i} size={13} fill="var(--amber-400)" stroke="none" />)}
                </div>
                <p style={{ fontSize:'0.9375rem', lineHeight:1.75, color:'rgba(255,255,255,0.75)', marginBottom:'1.25rem', fontStyle:'italic' }}>"{text}"</p>
                <div style={{ display:'flex', alignItems:'center', gap:'0.75rem' }}>
                  <div style={{ width:38, height:38, borderRadius:'50%', background:'linear-gradient(135deg,var(--teal-600),var(--teal-800))', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, color:'#fff', fontSize:'0.875rem', flexShrink:0 }}>{name[0]}</div>
                  <div>
                    <div style={{ fontWeight:700, color:'#fff', fontSize:'0.875rem' }}>{name}</div>
                    <div style={{ fontSize:'0.75rem', color:'rgba(255,255,255,0.4)' }}>{role} · {city}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" style={{ padding:'5rem 0', background:'#fff' }}>
        <div style={{ maxWidth:800, margin:'0 auto', padding:'0 1.5rem' }}>
          <div style={{ textAlign:'center', marginBottom:'3rem' }}>
            <div style={{ display:'inline-block', background:'var(--navy-100)', color:'var(--navy-600)', fontSize:'0.8125rem', fontWeight:700, padding:'0.375rem 1rem', borderRadius:999, marginBottom:'1rem' }}>FAQs</div>
            <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.25rem', marginBottom:'0.75rem' }}>
              Got Questions? <span style={{ color:'var(--teal-600)', fontStyle:'italic' }}>We've Got Answers.</span>
            </h2>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:'0.75rem' }}>
            {FAQS.map(({ q, a }) => <FaqItem key={q} q={q} a={a} />)}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section style={{ padding:'5rem 0', background:'linear-gradient(135deg,var(--teal-800),var(--navy-900))' }}>
        <div style={{ maxWidth:700, margin:'0 auto', padding:'0 1.5rem', textAlign:'center' }}>
          <h2 style={{ fontFamily:'var(--ff-display)', fontSize:'2.5rem', color:'#fff', marginBottom:'1rem', lineHeight:1.2 }}>
            Ready to get your loan <span style={{ color:'var(--teal-300)', fontStyle:'italic' }}>approved today?</span>
          </h2>
          <p style={{ color:'rgba(255,255,255,0.55)', fontSize:'1rem', marginBottom:'2rem' }}>
            Join 2.5 lakh+ customers who chose FinFlow for transparent, fast, and fair lending.
          </p>
          <div style={{ display:'flex', gap:'1rem', justifyContent:'center', flexWrap:'wrap' }}>
            <button className="btn btn-amber btn-xl" onClick={handleApply}>
              Apply Now — It's Free <ArrowRight size={18} />
            </button>
            <Link to="/signup" className="btn btn-xl" style={{ background:'rgba(255,255,255,0.08)', color:'#fff', borderColor:'rgba(255,255,255,0.2)' }}>
              Create Account
            </Link>
          </div>
          <div style={{ display:'flex', gap:'1.5rem', justifyContent:'center', marginTop:'1.75rem', flexWrap:'wrap' }}>
            {['No credit card required','100% digital process','Instant CIBIL check'].map(t => (
              <div key={t} style={{ display:'flex', alignItems:'center', gap:'0.375rem', color:'rgba(255,255,255,0.45)', fontSize:'0.875rem' }}>
                <CheckCircle2 size={13} style={{ color:'var(--teal-400)' }} /> {t}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer style={{ background:'var(--navy-950)', padding:'2rem 1.5rem', textAlign:'center' }}>
        <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:'0.5rem', marginBottom:'0.75rem' }}>
          <div style={{ width:28, height:28, background:'linear-gradient(135deg,var(--teal-500),var(--teal-700))', borderRadius:'var(--radius-sm)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:800, fontSize:'0.7rem', color:'#fff' }}>FF</div>
          <span style={{ fontWeight:800, fontSize:'1rem', color:'#fff' }}>Fin<span style={{ color:'var(--teal-400)' }}>Flow</span></span>
        </div>
        <p style={{ color:'rgba(255,255,255,0.25)', fontSize:'0.8rem' }}>
          © 2025 FinFlow Financial Services Pvt. Ltd. · RBI Registered NBFC ·{' '}
          <a href="#" style={{ color:'rgba(255,255,255,0.35)' }}>Privacy Policy</a> ·{' '}
          <a href="#" style={{ color:'rgba(255,255,255,0.35)' }}>Terms of Use</a>
        </p>
      </footer>
    </div>
  );
}
