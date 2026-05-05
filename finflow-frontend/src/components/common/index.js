import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';

// ── Status Badge ──────────────────────────────────────────────────────────────
export function StatusBadge({ status }) {
  const s = (status || '').toLowerCase();
  return <span className={`badge badge-${s}`}>{status?.replace(/_/g, ' ')}</span>;
}

// ── Alert ─────────────────────────────────────────────────────────────────────
const ALERT_ICONS = {
  error:   <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 1 }} />,
  success: <CheckCircle2 size={16} style={{ flexShrink: 0, marginTop: 1 }} />,
  info:    <Info size={16} style={{ flexShrink: 0, marginTop: 1 }} />,
  warning: <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: 1 }} />,
};

export function Alert({ type = 'info', children, onClose }) {
  return (
    <div className={`alert alert-${type}`}>
      {ALERT_ICONS[type]}
      <div style={{ flex: 1 }}>{children}</div>
      {onClose && (
        <button onClick={onClose} className="btn btn-ghost btn-sm" style={{ padding: '2px', marginTop: -2 }}>
          <X size={14} />
        </button>
      )}
    </div>
  );
}

// ── Spinner ───────────────────────────────────────────────────────────────────
export function Spinner({ size = '' }) {
  return <span className={`spinner ${size ? `spinner-${size}` : ''}`} />;
}

// ── Page Loader ───────────────────────────────────────────────────────────────
export function PageLoader({ text = 'Loading...' }) {
  return (
    <div className="page-loader">
      <Spinner size="lg" />
      <span style={{ color: 'var(--navy-400)', fontSize: '0.875rem' }}>{text}</span>
    </div>
  );
}

// ── Empty State ───────────────────────────────────────────────────────────────
export function EmptyState({ icon, title, description, action }) {
  return (
    <div className="empty-state">
      {icon && <div className="empty-state-icon">{icon}</div>}
      <div className="empty-state-title">{title}</div>
      {description && <p style={{ fontSize: '0.875rem', maxWidth: 320 }}>{description}</p>}
      {action}
    </div>
  );
}

// ── Modal ─────────────────────────────────────────────────────────────────────
export function Modal({ open, onClose, title, children, footer, maxWidth = 560 }) {
  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className="modal" style={{ maxWidth }}>
        <div className="modal-header">
          <h3 style={{ fontSize: '1.125rem' }}>{title}</h3>
          {onClose && (
            <button className="btn btn-ghost btn-sm" onClick={onClose} style={{ padding: 4 }}>
              <X size={18} />
            </button>
          )}
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

// ── Card ──────────────────────────────────────────────────────────────────────
export function Card({ children, style, className = '' }) {
  return <div className={`card ${className}`} style={style}>{children}</div>;
}

// ── Confirm dialog ────────────────────────────────────────────────────────────
export function ConfirmModal({ open, onClose, onConfirm, title, message, confirmLabel = 'Confirm', danger = false, loading = false }) {
  return (
    <Modal open={open} onClose={onClose} title={title}
      footer={
        <>
          <button className="btn btn-secondary" onClick={onClose} disabled={loading}>Cancel</button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={loading}>
            {loading ? <><Spinner size="sm" /> Processing...</> : confirmLabel}
          </button>
        </>
      }
    >
      <p style={{ color: 'var(--navy-600)', lineHeight: 1.6 }}>{message}</p>
    </Modal>
  );
}

// ── Section header ────────────────────────────────────────────────────────────
export function SectionHeader({ title, subtitle, action }) {
  return (
    <div className="section-header">
      <div>
        <div className="section-title">{title}</div>
        {subtitle && <div className="section-subtitle">{subtitle}</div>}
      </div>
      {action}
    </div>
  );
}
