import React, { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, FileText, Upload, Bell, LogOut,
  Users, ShieldCheck, BarChart3, Menu,
  ClipboardList, Inbox, Home, TrendingUp, X
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { notificationApi } from '../../api/services';
import { initials } from '../../utils/helpers';

const APPLICANT_NAV = [
  { to: '/dashboard',    icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/applications', icon: FileText,        label: 'My Applications' },
  { to: '/documents',    icon: Upload,          label: 'Documents' },
  { to: '/notifications',icon: Bell,            label: 'Notifications' },
];

const ADMIN_NAV = [
  { to: '/admin/dashboard',    icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/admin/applications', icon: ClipboardList,   label: 'Applications' },
  { to: '/admin/documents',    icon: Inbox,           label: 'Document Queue' },
  { to: '/admin/users',        icon: Users,           label: 'Users' },
  { to: '/admin/decisions',    icon: ShieldCheck,     label: 'Decisions' },
  { to: '/admin/reports',      icon: BarChart3,       label: 'Reports' },
  { to: '/notifications',      icon: Bell,            label: 'Notifications' },
];

export default function AppShell({ children }) {
  const { user, logout, isAdmin } = useAuth();
  const navigate  = useNavigate();
  const location  = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const navItems  = isAdmin ? ADMIN_NAV : APPLICANT_NAV;
  const pageTitle = navItems.find(n => location.pathname.startsWith(n.to))?.label || 'FinFlow';

  useEffect(() => {
    const fetch = () => notificationApi.getCount().then(r => setUnreadCount(r.data.unreadCount || 0)).catch(() => {});
    fetch();
    const id = setInterval(fetch, 30_000);
    return () => clearInterval(id);
  }, [location.pathname]);

  // Close sidebar on route change (mobile)
  useEffect(() => { setSidebarOpen(false); }, [location.pathname]);

  const handleLogout = () => { logout(); navigate('/'); };

  return (
    <div className="app-shell">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          style={{ position:'fixed', inset:0, background:'rgba(10,15,30,0.55)', zIndex:25, backdropFilter:'blur(3px)' }}
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <NavLink to="/" className="sidebar-logo">
          <div className="sidebar-logo-icon">FF</div>
          <div className="sidebar-logo-text">Fin<span>Flow</span></div>
        </NavLink>

        <nav className="sidebar-nav">
          <div className="nav-section-label">{isAdmin ? 'Administration' : 'Menu'}</div>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <Icon size={16} className="nav-icon" />
              <span style={{ flex:1 }}>{label}</span>
              {label === 'Notifications' && unreadCount > 0 && (
                <span className="notif-dot">{unreadCount > 99 ? '99+' : unreadCount}</span>
              )}
            </NavLink>
          ))}

          {/* Home link */}
          <div className="nav-section-label" style={{ marginTop:'0.75rem' }}>General</div>
          <NavLink to="/" className="nav-link">
            <Home size={16} className="nav-icon" />
            <span style={{ flex:1 }}>Public Home</span>
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="user-menu" onClick={handleLogout} title="Sign out">
            <div className="user-avatar">{initials(user?.email || 'U')}</div>
            <div style={{ flex:1, minWidth:0 }}>
              <div className="user-name" style={{ overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                {user?.email}
              </div>
              <div className="user-role">{user?.role}</div>
            </div>
            <LogOut size={14} style={{ color:'var(--navy-500)', flexShrink:0 }} />
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <div style={{ display:'flex', alignItems:'center', gap:'0.75rem' }}>
            <button
              id="mobile-menu-btn"
              className="btn btn-ghost btn-sm"
              style={{ display:'none', padding:6 }}
              onClick={() => setSidebarOpen(o => !o)}
            >
              {sidebarOpen ? <X size={19} /> : <Menu size={19} />}
            </button>
            <div className="topbar-title">{pageTitle}</div>
          </div>

          <div className="topbar-actions">
            {/* CIBIL quick link for applicants */}
            {!isAdmin && (
              <NavLink to="/applications" className="btn btn-ghost btn-sm" style={{ gap:'0.375rem', fontSize:'0.8rem' }}>
                <TrendingUp size={15} />
                <span style={{ display:'none' }} id="cibil-label">CIBIL</span>
              </NavLink>
            )}
            <NavLink to="/notifications" className="btn btn-ghost btn-sm" style={{ position:'relative', padding:'6px 8px' }}>
              <Bell size={17} />
              {unreadCount > 0 && (
                <span className="notif-dot" style={{ position:'absolute', top:1, right:1, minWidth:'0.875rem', height:'0.875rem', fontSize:'0.6rem' }}>
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </NavLink>
          </div>
        </header>

        <main className="page-body">{children}</main>
      </div>

      <style>{`
        @media (max-width:1024px) { #mobile-menu-btn { display:flex !important; } }
        @media (min-width:600px)  { #cibil-label { display:inline !important; } }
      `}</style>
    </div>
  );
}
