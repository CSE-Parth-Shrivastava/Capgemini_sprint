# FinFlow Frontend

React.js frontend for the FinFlow Loan Management Platform.

## Prerequisites
- Node.js 18+
- npm or yarn
- FinFlow backend running (API Gateway on port 8080)

## Setup & Run

```bash
# Install dependencies
npm install

# Start development server (runs on http://localhost:3000)
npm start

# Build for production
npm run build
```

## Environment Configuration

The `.env` file controls the backend URL:

```
REACT_APP_API_URL=http://localhost:8080
```

Change this to your deployed API Gateway URL for production.

## Backend Requirements

Make sure these services are running before starting the frontend:

| Service             | Port |
|---------------------|------|
| Config Server       | 8888 |
| Eureka Server       | 8761 |
| API Gateway         | 8080 |
| Auth Service        | 8081 |
| Application Service | 8082 |
| Document Service    | 8083 |
| Admin Service       | 8084 |
| Notification Service| 8085 |

## Project Structure

```
src/
├── api/
│   ├── client.js          # Axios instance with JWT interceptors
│   └── services.js        # All API calls (auth, applications, docs, admin, notifications)
├── components/
│   ├── common/index.js    # Reusable UI: Alert, Modal, Badge, Spinner, etc.
│   └── layout/AppShell.js # Sidebar + topbar layout wrapper
├── context/
│   └── AuthContext.js     # JWT auth state, login/logout
├── pages/
│   ├── auth/
│   │   ├── LoginPage.js
│   │   └── SignupPage.js
│   ├── applicant/
│   │   ├── DashboardPage.js
│   │   ├── ApplicationsListPage.js
│   │   ├── ApplicationFormPage.js    # 4-step form with all loan types
│   │   ├── ApplicationDetailPage.js  # Detail + document upload + submit
│   │   ├── DocumentsPage.js
│   │   └── NotificationsPage.js
│   └── admin/
│       ├── AdminDashboardPage.js
│       ├── AdminApplicationsPage.js
│       ├── AdminApplicationDetailPage.js  # Verify docs + make decision
│       ├── AdminDocumentsPage.js          # Pending document queue
│       ├── AdminUsersPage.js
│       ├── AdminDecisionsPage.js
│       └── AdminReportsPage.js
└── utils/
    └── helpers.js         # Formatters, constants, EMI calculator
```

## Features

### Applicant
- Sign up / Log in (JWT auto-login)
- Create loan applications with 4-step form (Personal → Employment → Loan → Review)
- Loan types: Personal, Home, Car, Education, Business, Gold
- Live EMI preview during form fill
- Upload 4 required documents (IDENTITY_PROOF, INCOME_PROOF, ADDRESS_PROOF, BANK_STATEMENT)
- Submit application (triggers backend eligibility + EMI BLI checks)
- Track application status in real time
- In-app notifications panel with unread badge

### Admin
- Dashboard with stats (total, approved, rejected, pending docs, approval rate)
- Browse and search all applications
- Review and verify/reject individual documents
- Make APPROVED/REJECTED decision (only when all 4 docs verified)
- Manage users (change role, activate/deactivate)
- View all decisions history
- Reports page with approval rate bar chart
