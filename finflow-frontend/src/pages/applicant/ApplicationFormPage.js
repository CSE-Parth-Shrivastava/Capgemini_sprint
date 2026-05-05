import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Save, Calculator, CheckCircle2 } from 'lucide-react';
import { applicationApi } from '../../api/services';
import { extractError } from '../../api/client';
import { Alert, Spinner, Card } from '../../components/common';
import { LOAN_TYPES, EMPLOYMENT_TYPES, calcEmi, fmtCurrency } from '../../utils/helpers';

const STEPS = [
  { label: 'Personal',   desc: 'Your personal details' },
  { label: 'Employment', desc: 'Income & work info' },
  { label: 'Loan',       desc: 'Loan type & amount' },
  { label: 'Review',     desc: 'Confirm & save draft' },
];

const BLANK = {
  fullName: '', email: '', phone: '', address: '', dateOfBirth: '',
  employmentType: 'SALARIED', employerName: '', monthlyIncome: '',
  loanType: 'PERSONAL', loanAmount: '', tenureMonths: '', purpose: '',
  propertyValue: '', propertyType: '',
  vehiclePrice: '', vehicleModel: '',
  institutionName: '', courseName: '',
  businessType: '', businessAge: '',
  goldWeightGrams: '', goldMarketValue: '',
  coApplicantName: '', coApplicantIncome: '',
};

function Field({ label, required, hint, error, children }) {
  return (
    <div className="form-group">
      <label className="form-label">
        {label}{required && <span className="required"> *</span>}
      </label>
      {children}
      {hint  && !error && <span className="form-hint">{hint}</span>}
      {error && <span className="form-error">{error}</span>}
    </div>
  );
}

function EmiPreview({ loanType, loanAmount, tenureMonths }) {
  const meta = LOAN_TYPES.find(t => t.value === loanType);
  const rate = parseFloat(meta?.rate) || 14;
  const emi  = calcEmi(Number(loanAmount), rate, Number(tenureMonths));
  if (!loanAmount || !tenureMonths || emi <= 0) return null;
  return (
    <div style={{ background:'var(--teal-50)', border:'1px solid var(--teal-200)', borderRadius:'var(--radius-lg)', padding:'1rem 1.25rem', display:'flex', alignItems:'center', gap:'0.875rem' }}>
      <Calculator size={20} style={{ color:'var(--teal-600)', flexShrink:0 }} />
      <div>
        <div style={{ fontSize:'0.8rem', color:'var(--teal-600)', fontWeight:500 }}>Estimated Monthly EMI</div>
        <div style={{ fontSize:'1.5rem', fontWeight:700, color:'var(--teal-800)', lineHeight:1.1, marginTop:2 }}>{fmtCurrency(emi)}</div>
        <div style={{ fontSize:'0.75rem', color:'var(--teal-500)', marginTop:2 }}>
          At {meta?.rate} p.a. &middot; {tenureMonths} months &middot; {fmtCurrency(loanAmount)} principal
        </div>
      </div>
    </div>
  );
}

export default function ApplicationFormPage() {
  const navigate = useNavigate();
  const { id }   = useParams();
  const isEdit   = Boolean(id);

  const [step,     setStep]     = useState(0);
  const [form,     setForm]     = useState(BLANK);
  const [errors,   setErrors]   = useState({});
  const [apiError, setApiError] = useState('');
  const [saving,   setSaving]   = useState(false);
  const [loading,  setLoading]  = useState(isEdit);
  const [saved,    setSaved]    = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    applicationApi.getById(id)
      .then(r => {
        const d = r.data;
        const merged = { ...BLANK };
        Object.keys(BLANK).forEach(k => { if (d[k] != null) merged[k] = String(d[k]); });
        setForm(merged);
      })
      .catch(() => setApiError('Failed to load application.'))
      .finally(() => setLoading(false));
  }, [id, isEdit]);

  const set    = (k, v) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => { const n={...e}; delete n[k]; return n; }); setSaved(false); };
  const handle = e => set(e.target.name, e.target.value);

  const validateStep = (s) => {
    const e = {};
    if (s === 0) {
      if (!form.fullName.trim())  e.fullName    = 'Required';
      if (!form.email.trim())     e.email       = 'Required';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = 'Invalid email';
      if (!form.phone.trim())     e.phone       = 'Required';
      else if (!/^[6-9]\d{9}$/.test(form.phone)) e.phone = '10-digit Indian mobile required';
      if (!form.address.trim())   e.address     = 'Required';
      if (!form.dateOfBirth)      e.dateOfBirth = 'Required';
    }
    if (s === 1) {
      if (!form.employmentType) e.employmentType = 'Required';
      if (!form.monthlyIncome || Number(form.monthlyIncome) < 5000) e.monthlyIncome = 'Minimum ₹5,000';
    }
    if (s === 2) {
      if (!form.loanType)   e.loanType    = 'Select a loan type';
      if (!form.loanAmount  || Number(form.loanAmount) < 10000)  e.loanAmount  = 'Minimum ₹10,000';
      if (!form.tenureMonths|| Number(form.tenureMonths) < 3)     e.tenureMonths= 'Minimum 3 months';
      if (!form.purpose.trim()) e.purpose = 'Required';
      if (form.loanType === 'HOME'      && !form.propertyValue)    e.propertyValue   = 'Required for Home Loan';
      if (form.loanType === 'EDUCATION' && !form.institutionName.trim()) e.institutionName = 'Required for Education Loan';
      if (form.loanType === 'GOLD'      && !form.goldMarketValue)  e.goldMarketValue = 'Required for Gold Loan';
    }
    return e;
  };

  const buildPayload = () => {
    const p = { ...form };
    ['monthlyIncome','loanAmount','propertyValue','vehiclePrice','goldWeightGrams','goldMarketValue','coApplicantIncome']
      .forEach(k => { if (p[k] !== '') p[k] = Number(p[k]); else delete p[k]; });
    ['tenureMonths','businessAge']
      .forEach(k => { if (p[k] !== '') p[k] = parseInt(p[k],10); else delete p[k]; });
    Object.keys(p).forEach(k => { if (p[k] === '' || p[k] == null) delete p[k]; });
    return p;
  };

  const saveAsDraft = async () => {
    setApiError(''); setSaving(true);
    try {
      if (isEdit) {
        await applicationApi.update(id, buildPayload());
        setSaved(true);
        setTimeout(() => navigate(`/applications/${id}`), 800);
      } else {
        const r = await applicationApi.create(buildPayload());
        setSaved(true);
        setTimeout(() => navigate(`/applications/${r.data.id}`), 800);
      }
    } catch (err) { setApiError(extractError(err)); }
    finally { setSaving(false); }
  };

  const goNext = () => {
    const e = validateStep(step);
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({}); setStep(s => s + 1); window.scrollTo(0,0);
  };
  const goBack = () => { setErrors({}); setApiError(''); setStep(s => s-1); window.scrollTo(0,0); };

  if (loading) return <div className="page-loader"><span className="spinner spinner-lg" /></div>;

  const loanMeta = LOAN_TYPES.find(t => t.value === form.loanType) || LOAN_TYPES[0];

  return (
    <div style={{ maxWidth: 780 }}>
      {/* Header */}
      <div style={{ display:'flex', alignItems:'center', gap:'0.875rem', marginBottom:'1.5rem' }}>
        <button className="btn btn-ghost btn-sm" style={{ padding:'6px 8px' }}
          onClick={() => navigate(isEdit ? `/applications/${id}` : '/applications')}>
          <ChevronLeft size={16} />
        </button>
        <div>
          <h2 style={{ fontSize:'1.25rem' }}>{isEdit ? 'Edit Application' : 'New Loan Application'}</h2>
          <p style={{ fontSize:'0.8125rem', color:'var(--navy-500)', marginTop:2 }}>
            Step {step+1} of {STEPS.length} — {STEPS[step].desc}
          </p>
        </div>
      </div>

      {/* Steps */}
      <div style={{ display:'flex', alignItems:'flex-start', marginBottom:'1.75rem' }}>
        {STEPS.map((s, i) => (
          <React.Fragment key={s.label}>
            <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
              <div className={`step-circle ${i < step ? 'done' : i === step ? 'active' : ''}`}>
                {i < step ? <CheckCircle2 size={14}/> : i+1}
              </div>
              <span className={`step-label ${i===step ? 'active' : ''}`} style={{ fontSize:'0.75rem', whiteSpace:'nowrap' }}>
                {s.label}
              </span>
            </div>
            {i < STEPS.length-1 && (
              <div className={`step-line ${i < step ? 'done' : ''}`} style={{ flex:1, margin:'1rem 4px 0' }} />
            )}
          </React.Fragment>
        ))}
      </div>

      {apiError && <Alert type="error" onClose={() => setApiError('')} style={{ marginBottom:'1rem' }}>{apiError}</Alert>}
      {saved    && <Alert type="success" style={{ marginBottom:'1rem' }}><CheckCircle2 size={14}/> Saved! Redirecting…</Alert>}

      <Card style={{ padding:'1.75rem' }}>

        {/* ── Step 0 ── */}
        {step === 0 && (
          <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
            <h3>Personal Information</h3>
            <div className="form-grid-2">
              <Field label="Full name" required error={errors.fullName}>
                <input className={`form-input ${errors.fullName?'error':''}`} name="fullName"
                  value={form.fullName} onChange={handle} placeholder="Raj Kumar Sharma" autoFocus />
              </Field>
              <Field label="Date of birth" required error={errors.dateOfBirth} hint="Must be 21–65 years old">
                <input className={`form-input ${errors.dateOfBirth?'error':''}`} name="dateOfBirth"
                  type="date" value={form.dateOfBirth} onChange={handle}
                  max={new Date(Date.now()-21*365.25*86400000).toISOString().split('T')[0]} />
              </Field>
            </div>
            <div className="form-grid-2">
              <Field label="Email address" required error={errors.email}>
                <input className={`form-input ${errors.email?'error':''}`} name="email"
                  type="email" value={form.email} onChange={handle} placeholder="raj@gmail.com" />
              </Field>
              <Field label="Mobile number" required error={errors.phone} hint="10-digit, starts with 6–9">
                <input className={`form-input ${errors.phone?'error':''}`} name="phone"
                  value={form.phone} onChange={handle} placeholder="9876543210" maxLength={10} />
              </Field>
            </div>
            <Field label="Residential address" required error={errors.address}>
              <textarea className={`form-textarea ${errors.address?'error':''}`} name="address"
                value={form.address} onChange={handle}
                placeholder="123, MG Road, Pune, Maharashtra – 411001" rows={3} />
            </Field>
          </div>
        )}

        {/* ── Step 1 ── */}
        {step === 1 && (
          <div style={{ display:'flex', flexDirection:'column', gap:'1rem' }}>
            <h3>Employment & Income</h3>
            <div className="form-grid-2">
              <Field label="Employment type" required error={errors.employmentType}>
                <select className={`form-select ${errors.employmentType?'error':''}`}
                  name="employmentType" value={form.employmentType} onChange={handle}>
                  {EMPLOYMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </Field>
              <Field label="Net monthly income (₹)" required error={errors.monthlyIncome} hint="Min ₹25,000 for Home Loan">
                <input className={`form-input ${errors.monthlyIncome?'error':''}`} name="monthlyIncome"
                  type="number" min="5000" value={form.monthlyIncome} onChange={handle} placeholder="75000" />
              </Field>
            </div>
            {!['UNEMPLOYED','STUDENT','RETIRED'].includes(form.employmentType) && (
              <Field label="Employer / business name">
                <input className="form-input" name="employerName"
                  value={form.employerName} onChange={handle} placeholder="Infosys Ltd." />
              </Field>
            )}
            {['BUSINESS_OWNER','SELF_EMPLOYED'].includes(form.employmentType) && (
              <div className="form-grid-2">
                <Field label="Business type / industry">
                  <input className="form-input" name="businessType"
                    value={form.businessType} onChange={handle} placeholder="Retail — Clothing" />
                </Field>
                <Field label="Years in business" hint="Min. 2 years for Business Loan">
                  <input className="form-input" name="businessAge" type="number" min="0"
                    value={form.businessAge} onChange={handle} placeholder="3" />
                </Field>
              </div>
            )}
            <div style={{ borderTop:'1px solid var(--navy-100)', paddingTop:'1.25rem' }}>
              <div style={{ fontWeight:600, fontSize:'0.9375rem', color:'var(--navy-700)', marginBottom:'0.875rem' }}>
                Co-applicant <span style={{ fontWeight:400, color:'var(--navy-400)', fontSize:'0.8125rem' }}>(optional)</span>
              </div>
              <div className="form-grid-2">
                <Field label="Co-applicant name" hint="Spouse or parent — improves Home Loan eligibility">
                  <input className="form-input" name="coApplicantName"
                    value={form.coApplicantName} onChange={handle} placeholder="Priya Raj Sharma" />
                </Field>
                <Field label="Co-applicant monthly income (₹)">
                  <input className="form-input" name="coApplicantIncome" type="number" min="0"
                    value={form.coApplicantIncome} onChange={handle} placeholder="50000" />
                </Field>
              </div>
            </div>
          </div>
        )}

        {/* ── Step 2 ── */}
        {step === 2 && (
          <div style={{ display:'flex', flexDirection:'column', gap:'1.125rem' }}>
            <h3>Loan Details</h3>
            <Field label="Loan type" required error={errors.loanType}>
              <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:'0.625rem' }}>
                {LOAN_TYPES.map(t => {
                  const active = form.loanType === t.value;
                  return (
                    <button key={t.value} type="button" onClick={() => set('loanType',t.value)}
                      style={{ padding:'0.875rem 0.5rem', borderRadius:'var(--radius)', border:`2px solid ${active?'var(--teal-600)':'var(--navy-200)'}`, background:active?'var(--teal-50)':'#fff', cursor:'pointer', textAlign:'center', transition:'all 0.15s' }}>
                      <div style={{ fontSize:'0.875rem', fontWeight:600, color:active?'var(--teal-700)':'var(--navy-800)' }}>{t.label}</div>
                      <div style={{ fontSize:'0.75rem', color:active?'var(--teal-500)':'var(--navy-400)', marginTop:2 }}>{t.rate} p.a.</div>
                    </button>
                  );
                })}
              </div>
            </Field>
            <div className="form-grid-2">
              <Field label="Loan amount (₹)" required error={errors.loanAmount} hint="₹10,000 – ₹5 Crore">
                <input className={`form-input ${errors.loanAmount?'error':''}`} name="loanAmount"
                  type="number" min="10000" max="50000000" value={form.loanAmount} onChange={handle} placeholder="500000" />
              </Field>
              <Field label="Tenure (months)" required error={errors.tenureMonths}
                hint={`${loanMeta.minTenure}–${loanMeta.maxTenure} months allowed`}>
                <input className={`form-input ${errors.tenureMonths?'error':''}`} name="tenureMonths"
                  type="number" min={loanMeta.minTenure} max={loanMeta.maxTenure}
                  value={form.tenureMonths} onChange={handle} placeholder={String(loanMeta.minTenure)} />
              </Field>
            </div>
            <Field label="Purpose of loan" required error={errors.purpose}>
              <input className={`form-input ${errors.purpose?'error':''}`} name="purpose"
                value={form.purpose} onChange={handle}
                placeholder="Home renovation / Purchase of Honda City / Higher education at IIT" />
            </Field>
            {form.loanType === 'HOME' && (
              <div className="form-grid-2">
                <Field label="Property value (₹)" required error={errors.propertyValue} hint="Loan ≤ 80% of property value">
                  <input className={`form-input ${errors.propertyValue?'error':''}`} name="propertyValue"
                    type="number" min="0" value={form.propertyValue} onChange={handle} placeholder="6000000" />
                </Field>
                <Field label="Property type">
                  <select className="form-select" name="propertyType" value={form.propertyType} onChange={handle}>
                    <option value="">Select type</option>
                    {['FLAT','HOUSE','PLOT','UNDER_CONSTRUCTION'].map(v => (
                      <option key={v} value={v}>{v.replace('_',' ')}</option>
                    ))}
                  </select>
                </Field>
              </div>
            )}
            {form.loanType === 'CAR' && (
              <div className="form-grid-2">
                <Field label="Vehicle on-road price (₹)" hint="Loan ≤ 85% of vehicle price">
                  <input className="form-input" name="vehiclePrice" type="number" min="0"
                    value={form.vehiclePrice} onChange={handle} placeholder="1200000" />
                </Field>
                <Field label="Vehicle make & model">
                  <input className="form-input" name="vehicleModel"
                    value={form.vehicleModel} onChange={handle} placeholder="Maruti Suzuki Swift ZXI" />
                </Field>
              </div>
            )}
            {form.loanType === 'EDUCATION' && (
              <div className="form-grid-2">
                <Field label="Institution name" required error={errors.institutionName}>
                  <input className={`form-input ${errors.institutionName?'error':''}`} name="institutionName"
                    value={form.institutionName} onChange={handle} placeholder="IIT Bombay" />
                </Field>
                <Field label="Course name & duration">
                  <input className="form-input" name="courseName"
                    value={form.courseName} onChange={handle} placeholder="B.Tech Computer Science, 4 years" />
                </Field>
              </div>
            )}
            {form.loanType === 'GOLD' && (
              <div className="form-grid-2">
                <Field label="Gold weight (grams)" required>
                  <input className="form-input" name="goldWeightGrams" type="number" min="0"
                    value={form.goldWeightGrams} onChange={handle} placeholder="50" />
                </Field>
                <Field label="Gold market value (₹)" required error={errors.goldMarketValue} hint="Loan ≤ 75% of gold value (RBI)">
                  <input className={`form-input ${errors.goldMarketValue?'error':''}`} name="goldMarketValue"
                    type="number" min="0" value={form.goldMarketValue} onChange={handle} placeholder="300000" />
                </Field>
              </div>
            )}
            <EmiPreview loanType={form.loanType} loanAmount={form.loanAmount} tenureMonths={form.tenureMonths} />
          </div>
        )}

        {/* ── Step 3: Review ── */}
        {step === 3 && (
          <div style={{ display:'flex', flexDirection:'column', gap:'1.25rem' }}>
            <div>
              <h3>Review Your Application</h3>
              <p style={{ fontSize:'0.875rem', color:'var(--navy-500)', marginTop:4 }}>Check all details before saving.</p>
            </div>
            {[
              { title:'Personal', rows:[['Full name',form.fullName],['Date of birth',form.dateOfBirth],['Email',form.email],['Mobile',form.phone],['Address',form.address]] },
              { title:'Employment', rows:[['Type',EMPLOYMENT_TYPES.find(t=>t.value===form.employmentType)?.label],['Employer',form.employerName||null],['Monthly income',form.monthlyIncome?fmtCurrency(Number(form.monthlyIncome)):null],form.coApplicantName?['Co-applicant',form.coApplicantName]:null].filter(Boolean) },
              { title:'Loan', rows:[['Type',loanMeta.label],['Amount',form.loanAmount?fmtCurrency(Number(form.loanAmount)):null],['Tenure',form.tenureMonths?`${form.tenureMonths} months`:null],['Rate',loanMeta.rate+' p.a.'],['Purpose',form.purpose],form.propertyValue?['Property value',fmtCurrency(Number(form.propertyValue))]:null,form.vehicleModel?['Vehicle',form.vehicleModel]:null,form.institutionName?['Institution',form.institutionName]:null,form.goldMarketValue?['Gold value',fmtCurrency(Number(form.goldMarketValue))]:null].filter(Boolean) },
            ].map(sec => (
              <div key={sec.title}>
                <div style={{ fontSize:'0.75rem', fontWeight:700, color:'var(--navy-400)', textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:'0.5rem' }}>{sec.title}</div>
                <div style={{ border:'1px solid var(--navy-200)', borderRadius:'var(--radius-lg)', overflow:'hidden', background:'var(--navy-50)' }}>
                  {sec.rows.map(([k,v]) => v ? (
                    <div key={k} style={{ display:'flex', padding:'0.5rem 1rem', borderBottom:'1px solid var(--navy-100)', fontSize:'0.875rem', gap:'1rem' }}>
                      <span style={{ width:160, flexShrink:0, color:'var(--navy-500)' }}>{k}</span>
                      <span style={{ fontWeight:500 }}>{v}</span>
                    </div>
                  ) : null)}
                </div>
              </div>
            ))}
            <EmiPreview loanType={form.loanType} loanAmount={form.loanAmount} tenureMonths={form.tenureMonths} />
            <Alert type="info">
              Saving creates a <strong>Draft</strong>. After saving, go to the application page to upload your 4 required documents, then submit.
            </Alert>
          </div>
        )}

        {/* Nav */}
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginTop:'1.75rem', paddingTop:'1.5rem', borderTop:'1px solid var(--navy-100)' }}>
          <button className="btn btn-secondary" onClick={goBack} disabled={step===0||saving}>
            <ChevronLeft size={15}/> Back
          </button>
          <div style={{ display:'flex', gap:'0.75rem' }}>
            {step > 0 && step < STEPS.length-1 && (
              <button className="btn btn-secondary" onClick={saveAsDraft} disabled={saving}>
                {saving ? <><Spinner size="sm"/> Saving…</> : <><Save size={14}/> Save Draft</>}
              </button>
            )}
            {step < STEPS.length-1 ? (
              <button className="btn btn-primary" onClick={goNext} disabled={saving}>
                Next <ChevronRight size={15}/>
              </button>
            ) : (
              <button className="btn btn-primary btn-lg" onClick={saveAsDraft} disabled={saving}>
                {saving ? <><Spinner size="sm"/> Saving…</> : <><Save size={15}/> Save as Draft</>}
              </button>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
