import React, { useState, useEffect } from 'react';
import { Bell, CheckCheck, Circle } from 'lucide-react';
import { notificationApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, Card, SectionHeader } from '../../components/common';
import { fmtTimeAgo } from '../../utils/helpers';

const EVENT_COLORS = {
  APPLICATION_CREATED:   'var(--blue-500)',
  APPLICATION_SUBMITTED: 'var(--teal-600)',
  APPLICATION_APPROVED:  'var(--green-600)',
  APPLICATION_REJECTED:  'var(--red-500)',
  DOCUMENT_UPLOADED:     'var(--yellow-500)',
  DOCUMENT_VERIFIED:     'var(--green-500)',
  DOCUMENT_REJECTED:     'var(--red-500)',
  SIGNUP_SUCCESS:        'var(--teal-500)',
  LOGIN_SUCCESS:         'var(--navy-400)',
};

export default function NotificationsPage() {
  const [notifs,  setNotifs]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [marking, setMarking] = useState(false);

  const load = () => {
    notificationApi.getAll()
      .then(r => setNotifs(r.data))
      .catch(() => setError('Failed to load notifications.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const markAll = async () => {
    setMarking(true);
    try {
      await notificationApi.markAllRead();
      setNotifs(n => n.map(x => ({ ...x, read: true })));
    } catch(err) { setError(extractError(err)); }
    finally { setMarking(false); }
  };

  const markOne = async (id) => {
    try {
      await notificationApi.markRead(id);
      setNotifs(n => n.map(x => x.id === id ? { ...x, read: true } : x));
    } catch {}
  };

  if (loading) return <PageLoader />;

  const unread = notifs.filter(n => !n.read).length;

  return (
    <div style={{ maxWidth: 720 }}>
      <SectionHeader
        title="Notifications"
        subtitle={unread > 0 ? `${unread} unread` : 'All caught up'}
        action={
          unread > 0 && (
            <button className="btn btn-secondary btn-sm" onClick={markAll} disabled={marking}>
              <CheckCheck size={14} /> {marking ? 'Marking...' : 'Mark all read'}
            </button>
          )
        }
      />

      {error && <Alert type="error" style={{ marginBottom:'1rem' }}>{error}</Alert>}

      {notifs.length === 0 ? (
        <Card>
          <div className="empty-state">
            <Bell size={40} style={{ color:'var(--navy-300)' }} />
            <div className="empty-state-title">No notifications yet</div>
            <p style={{ fontSize:'0.875rem' }}>Notifications will appear here as you use FinFlow.</p>
          </div>
        </Card>
      ) : (
        <Card>
          {notifs.map((n, i) => (
            <div
              key={n.id}
              style={{
                display:'flex', gap:'0.875rem', padding:'1rem 1.25rem',
                borderBottom: i < notifs.length - 1 ? '1px solid var(--navy-100)' : 'none',
                background: n.read ? 'transparent' : 'var(--teal-50)',
                cursor: !n.read ? 'pointer' : 'default',
                transition: 'background 0.15s',
              }}
              onClick={() => !n.read && markOne(n.id)}
            >
              {/* Dot */}
              <div style={{ paddingTop: 4, flexShrink: 0 }}>
                {n.read
                  ? <Circle size={8} style={{ color:'var(--navy-300)', fill:'var(--navy-300)' }} />
                  : <Circle size={8} style={{ color:'var(--teal-600)', fill:'var(--teal-600)' }} />
                }
              </div>

              {/* Color bar */}
              <div style={{ width:3, borderRadius:99, background: EVENT_COLORS[n.eventType] || 'var(--navy-300)', flexShrink:0, minHeight:36 }} />

              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:'0.5rem' }}>
                  <div style={{ fontWeight: n.read ? 400 : 600, fontSize:'0.9375rem', color:'var(--navy-900)' }}>{n.subject}</div>
                  <div style={{ fontSize:'0.75rem', color:'var(--navy-400)', whiteSpace:'nowrap', flexShrink:0 }}>{fmtTimeAgo(n.createdAt)}</div>
                </div>
                <p style={{ fontSize:'0.875rem', color:'var(--navy-600)', marginTop:'0.25rem', lineHeight:1.6 }}>{n.message}</p>
                <div style={{ display:'flex', gap:'0.5rem', marginTop:'0.375rem' }}>
                  {n.eventType && (
                    <span style={{ fontSize:'0.6875rem', fontWeight:600, color:'var(--navy-400)', textTransform:'uppercase', letterSpacing:'0.05em' }}>
                      {n.eventType.replace(/_/g,' ')}
                    </span>
                  )}
                  {n.applicationId && (
                    <span style={{ fontSize:'0.6875rem', color:'var(--navy-400)' }}>· App #{n.applicationId}</span>
                  )}
                  {n.type && (
                    <span style={{ fontSize:'0.6875rem', color:'var(--navy-400)' }}>· {n.type}</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
