import api from './client';

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  login:            (data)       => api.post('/auth/login', data),
  signup:           (data)       => api.post('/auth/signup', data),
  getUsers:         ()           => api.get('/auth/users'),
  updateUserStatus: (id, active) => api.put(`/auth/users/${id}`, { active }),
  updateUserRole:   (id, role)   => api.put(`/auth/users/${id}/role`, { role }),
};

// ── Applications ──────────────────────────────────────────────────────────────
export const applicationApi = {
  create:           (data)               => api.post('/applications', data),
  update:           (id, data)           => api.put(`/applications/${id}`, data),
  submit:           (id)                 => api.post(`/applications/${id}/submit`),
  getMyList:        ()                   => api.get('/applications/my'),
  getById:          (id)                 => api.get(`/applications/${id}`),
  getStatus:        (id)                 => api.get(`/applications/${id}/status`),
  getAll:           ()                   => api.get('/applications'),         // admin
  updateStatus:     (id, status, remarks) => api.put(`/applications/${id}/status`, { status, remarks }),
  getStatusHistory: (id)                 => api.get(`/applications/${id}/status-history`),
};

// ── Credit Score ──────────────────────────────────────────────────────────────
export const creditScoreApi = {
  assess:     (id, data) => api.post(`/applications/${id}/credit-score`, data),
  getLatest:  (id)       => api.get(`/applications/${id}/credit-score`),
  getHistory: (id)       => api.get(`/applications/${id}/credit-score/history`),
};

// ── Documents ─────────────────────────────────────────────────────────────────
export const documentApi = {
  upload: (applicationId, documentType, file) => {
    const form = new FormData();
    form.append('applicationId', applicationId);
    form.append('documentType', documentType);
    form.append('file', file);
    return api.post('/documents/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getByApplication: (applicationId) => api.get(`/documents/application/${applicationId}`),
  getMyDocuments:   ()               => api.get('/documents/my'),
  getPending:       ()               => api.get('/documents/pending'),   // admin
  verify: (id, approved, remarks)    => api.put(`/documents/${id}/verify`, { approved, remarks }),
};

// ── Admin ─────────────────────────────────────────────────────────────────────
export const adminApi = {
  makeDecision: (applicationId, data) =>
    api.post(`/admin/applications/${applicationId}/decision`, data),

  /**
   * Fetch the admin decision for a given application.
   * Returns the decision record (remarks, approvedAmount, interestRate, tenureMonths, decidedAt).
   * Throws a 404 axios error if no decision has been recorded yet — callers should catch this.
   */
  getDecisionByApp: (applicationId) =>
    api.get(`/admin/applications/${applicationId}/decision`),

  getReports:   () => api.get('/admin/reports'),
  getDecisions: () => api.get('/admin/decisions'),
};

// ── Notifications ─────────────────────────────────────────────────────────────
export const notificationApi = {
  getAll:      () => api.get('/notifications/my'),
  getUnread:   () => api.get('/notifications/my/unread'),
  getCount:    () => api.get('/notifications/my/count'),
  markRead:    (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put('/notifications/my/read-all'),
};