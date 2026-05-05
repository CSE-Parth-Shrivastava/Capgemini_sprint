import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

import AppShell from './components/layout/AppShell';
import HomePage  from './pages/home/HomePage';

// Auth
import LoginPage  from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';

// Applicant
import DashboardPage         from './pages/applicant/DashboardPage';
import ApplicationsListPage  from './pages/applicant/ApplicationsListPage';
import ApplicationFormPage   from './pages/applicant/ApplicationFormPage';
import ApplicationDetailPage from './pages/applicant/ApplicationDetailPage';
import ApplicationStatusPage from './pages/applicant/ApplicationStatusPage';
import CreditScorePage       from './pages/applicant/CreditScorePage';
import DocumentsPage         from './pages/applicant/DocumentsPage';
import NotificationsPage     from './pages/applicant/NotificationsPage';

// Admin
import AdminDashboardPage        from './pages/admin/AdminDashboardPage';
import AdminApplicationsPage     from './pages/admin/AdminApplicationsPage';
import AdminApplicationDetailPage from './pages/admin/AdminApplicationDetailPage';
import AdminDocumentsPage        from './pages/admin/AdminDocumentsPage';
import AdminUsersPage            from './pages/admin/AdminUsersPage';
import AdminDecisionsPage        from './pages/admin/AdminDecisionsPage';
import AdminReportsPage          from './pages/admin/AdminReportsPage';
import AdminCreditScorePage      from './pages/admin/AdminCreditScorePage';

// ── Guards ──────────────────────────────────────────────────────────────────

function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return null;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  return children;
}

function RequireAdmin({ children }) {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();
  if (loading) return null;
  if (!user)    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (!isAdmin) return <Navigate to="/dashboard" replace />;
  return children;
}

function RequireApplicant({ children }) {
  const { user, loading, isApplicant } = useAuth();
  const location = useLocation();
  if (loading) return null;
  if (!user)        return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (!isApplicant) return <Navigate to="/admin/dashboard" replace />;
  return children;
}

// ── Routes ───────────────────────────────────────────────────────────────────
function AppRoutes() {
  return (
    <Routes>
      {/* Public home — accessible to everyone */}
      <Route path="/" element={<HomePage />} />

      {/* Auth */}
      <Route path="/login"  element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      {/* Applicant */}
      <Route path="/dashboard" element={<RequireApplicant><AppShell><DashboardPage /></AppShell></RequireApplicant>} />
      <Route path="/applications" element={<RequireApplicant><AppShell><ApplicationsListPage /></AppShell></RequireApplicant>} />
      <Route path="/applications/new" element={<RequireApplicant><AppShell><ApplicationFormPage /></AppShell></RequireApplicant>} />
      <Route path="/applications/:id" element={<RequireApplicant><AppShell><ApplicationDetailPage /></AppShell></RequireApplicant>} />
      <Route path="/applications/:id/status" element={<RequireApplicant><AppShell><ApplicationStatusPage /></AppShell></RequireApplicant>} />
      <Route path="/applications/:id/credit-score" element={<RequireApplicant><AppShell><CreditScorePage /></AppShell></RequireApplicant>} />
      <Route path="/applications/:id/edit" element={<RequireApplicant><AppShell><ApplicationFormPage /></AppShell></RequireApplicant>} />
      <Route path="/documents" element={<RequireApplicant><AppShell><DocumentsPage /></AppShell></RequireApplicant>} />

      {/* Shared */}
      <Route path="/notifications" element={<RequireAuth><AppShell><NotificationsPage /></AppShell></RequireAuth>} />

      {/* Admin */}
      <Route path="/admin/dashboard" element={<RequireAdmin><AppShell><AdminDashboardPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/applications" element={<RequireAdmin><AppShell><AdminApplicationsPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/applications/:id" element={<RequireAdmin><AppShell><AdminApplicationDetailPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/applications/:id/credit-score" element={<RequireAdmin><AppShell><AdminCreditScorePage /></AppShell></RequireAdmin>} />
      <Route path="/admin/documents" element={<RequireAdmin><AppShell><AdminDocumentsPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/users" element={<RequireAdmin><AppShell><AdminUsersPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/decisions" element={<RequireAdmin><AppShell><AdminDecisionsPage /></AppShell></RequireAdmin>} />
      <Route path="/admin/reports" element={<RequireAdmin><AppShell><AdminReportsPage /></AppShell></RequireAdmin>} />

      {/* Catch-all → home */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
