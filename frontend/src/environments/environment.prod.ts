// Production environment configuration
// These values are replaced at build time via Docker build args
export const environment = {
  production: true,
  apiUrl: '${API_URL}',
  apiBaseUrl: '${API_BASE_URL}',
  frontendUrl: '${FRONTEND_URL}',

  // Feature flags
  enableDebugMode: false,
  enableLogging: false,

  // API endpoints
  endpoints: {
    auth: '/auth',
    users: '/users',
    employees: '/employees',
    organizations: '/organizations',
    departments: '/departments',
    leaves: '/leaves',
    documents: '/documents',
    rotas: '/rota',
    attendance: '/attendance',
    notifications: '/notifications',
    dashboard: '/dashboard'
  },

  // File upload limits
  // Support documents up to 20MB (high-resolution scans, multi-page PDFs)
  fileUpload: {
    maxSizeMB: 20,
    allowedDocumentTypes: ['.pdf', '.jpg', '.jpeg', '.png'],
    allowedImageTypes: ['.jpg', '.jpeg', '.png', '.gif']
  },

  // Pagination
  pagination: {
    defaultPageSize: 10,
    pageSizeOptions: [5, 10, 25, 50, 100]
  },

  // Session timeout (in minutes)
  sessionTimeout: 30,

  // Date formats
  dateFormat: 'yyyy-MM-dd',
  dateTimeFormat: 'yyyy-MM-dd HH:mm:ss',
  displayDateFormat: 'MMM dd, yyyy',

  // App metadata
  appName: 'Employee Management System',
  appVersion: '1.0.0',
  company: 'Your Company'
};

