// Development environment configuration
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  apiBaseUrl: 'http://localhost:8080',
  frontendUrl: 'http://localhost:4200',

  // Feature flags
  enableDebugMode: true,
  enableLogging: true,

  // API endpoints
  endpoints: {
    auth: '/auth',
    users: '/users',
    employees: '/employees',
    organizations: '/organizations',
    departments: '/departments',
    leaves: '/leaves',
    documents: '/documents',
    rotas: '/rotas',
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

