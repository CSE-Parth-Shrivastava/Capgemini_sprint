import React, { useState, useEffect } from 'react';
import { Users, ShieldCheck, UserX, UserCheck } from 'lucide-react';
import { authApi } from '../../api/services';
import { extractError } from '../../api/client';
import { PageLoader, Alert, Card, SectionHeader, Modal, Spinner } from '../../components/common';
import { fmtDateTime, initials } from '../../utils/helpers';

export default function AdminUsersPage() {
  const [users,   setUsers]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  const [roleModal,  setRoleModal]  = useState(false);
  const [targetUser, setTargetUser] = useState(null);
  const [newRole,    setNewRole]    = useState('');
  const [saving,     setSaving]     = useState(false);

  const load = () => {
    authApi.getUsers()
      .then(r => setUsers(r.data))
      .catch(() => setError('Failed to load users.'))
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);

  const toggleActive = async (user) => {
    try {
      await authApi.updateUserStatus(user.id, !user.active);
      setSuccess(`User ${user.active ? 'deactivated' : 'activated'}.`);
      load();
    } catch(err) { setError(extractError(err)); }
  };

  const openRoleChange = (user) => { setTargetUser(user); setNewRole(user.role); setRoleModal(true); };

  const handleRoleChange = async () => {
    setSaving(true);
    try {
      await authApi.updateUserRole(targetUser.id, newRole);
      setRoleModal(false);
      setSuccess(`Role updated to ${newRole}.`);
      load();
    } catch(err) { setError(extractError(err)); }
    finally { setSaving(false); }
  };

  if (loading) return <PageLoader />;

  return (
    <div>
      <SectionHeader title="User Management" subtitle={`${users.length} registered user${users.length !== 1 ? 's' : ''}`} />

      {error   && <Alert type="error"   onClose={() => setError('')}   style={{ marginBottom:'1rem' }}>{error}</Alert>}
      {success && <Alert type="success" onClose={() => setSuccess('')} style={{ marginBottom:'1rem' }}>{success}</Alert>}

      <Card>
        <div className="table-wrapper" style={{ border:'none', borderRadius:0 }}>
          <table>
            <thead>
              <tr><th>User</th><th>Phone</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr><td colSpan={6} style={{ textAlign:'center', padding:'3rem', color:'var(--navy-400)' }}>No users found</td></tr>
              ) : users.map(u => (
                <tr key={u.id}>
                  <td>
                    <div style={{ display:'flex', alignItems:'center', gap:'0.625rem' }}>
                      <div style={{ width:32, height:32, borderRadius:'50%', background:'var(--teal-100)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'0.75rem', fontWeight:700, color:'var(--teal-700)', flexShrink:0 }}>
                        {initials(u.fullName || u.email)}
                      </div>
                      <div>
                        <div style={{ fontWeight:500 }}>{u.fullName || '—'}</div>
                        <div style={{ fontSize:'0.75rem', color:'var(--navy-400)' }}>{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ color:'var(--navy-600)' }}>{u.phone || '—'}</td>
                  <td>
                    <span style={{ display:'inline-flex', alignItems:'center', gap:4, fontSize:'0.8125rem', fontWeight:600, color: u.role==='ADMIN' ? 'var(--teal-700)' : 'var(--navy-600)', background: u.role==='ADMIN' ? 'var(--teal-50)' : 'var(--navy-100)', padding:'2px 10px', borderRadius:99 }}>
                      {u.role === 'ADMIN' && <ShieldCheck size={12}/>}
                      {u.role}
                    </span>
                  </td>
                  <td>
                    <span style={{ display:'inline-flex', alignItems:'center', gap:4, fontSize:'0.8125rem', fontWeight:600, color: u.active ? 'var(--green-700)' : 'var(--red-600)', background: u.active ? 'var(--green-50)' : 'var(--red-50)', padding:'2px 10px', borderRadius:99 }}>
                      {u.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td style={{ color:'var(--navy-500)', fontSize:'0.8125rem' }}>{fmtDateTime(u.createdAt)}</td>
                  <td>
                    <div style={{ display:'flex', gap:'0.5rem' }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => openRoleChange(u)}>
                        <ShieldCheck size={12}/> Role
                      </button>
                      <button className={`btn btn-sm ${u.active ? 'btn-danger' : 'btn-success'}`} onClick={() => toggleActive(u)}>
                        {u.active ? <><UserX size={12}/> Deactivate</> : <><UserCheck size={12}/> Activate</>}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal open={roleModal} onClose={() => setRoleModal(false)} title="Change User Role"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setRoleModal(false)} disabled={saving}>Cancel</button>
            <button className="btn btn-primary" onClick={handleRoleChange} disabled={saving || newRole === targetUser?.role}>
              {saving ? <><Spinner size="sm"/> Saving...</> : 'Update Role'}
            </button>
          </>
        }
      >
        <p style={{ fontSize:'0.875rem', color:'var(--navy-600)', marginBottom:'1rem' }}>
          Changing role for <strong>{targetUser?.email}</strong>
        </p>
        <div className="form-group">
          <label className="form-label">New Role</label>
          <select className="form-select" value={newRole} onChange={e => setNewRole(e.target.value)}>
            <option value="APPLICANT">APPLICANT</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
      </Modal>
    </div>
  );
}
