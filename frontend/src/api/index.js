import http from './http'

export const login = (data) => http.post('/api/auth/login', data)

export const api = {
  regions: () => http.get('/api/basic/regions'),
  schools: (regionId) => http.get('/api/basic/schools', { params: { regionId } }),
  classes: (schoolId) => http.get('/api/basic/classes', { params: { schoolId } }),

  students: (params) => http.get('/api/students', { params }),
  studentDetail: (id) => http.get(`/api/students/${id}`),
  studentTimeline: (id) => http.get(`/api/students/${id}/timeline`),
  saveStudent: (data) => http.post('/api/students', data),
  importStudents: (formData) => http.post('/api/students/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  fixImport: (data) => http.post('/api/students/fix-import', data),
  importTemplateUrl: '/api/students/import-template',

  screen: (data) => http.post('/api/flow/screen', data),
  schoolReview: (data) => http.post('/api/flow/school-review', data),
  notify: (data) => http.post('/api/flow/notify', data),
  parentChoice: (data) => http.post('/api/flow/parent-choice', data),
  visit: (data) => http.post('/api/flow/visit', data),
  glasses: (data) => http.post('/api/flow/glasses', data),
  resolveDiscomfort: (id, note) => http.post(`/api/flow/glasses/${id}/resolve-discomfort`, null,
    { params: { note } }),
  subsidyApply: (data) => http.post('/api/flow/subsidy/apply', data),
  subsidyApprove: (data) => http.post('/api/flow/subsidy/approve', data),
  followup: (data) => http.post('/api/flow/followup', data),
  intervention: (data) => http.post('/api/flow/intervention', data),
  resolveIssue: (data) => http.post('/api/flow/issue/resolve', data),

  refusalRecord: (data) => http.post('/api/refusal/record', data),
  refusalCallback: (data) => http.post('/api/refusal/callback', data),
  jointApply: (data) => http.post('/api/refusal/joint/apply', data),
  jointReview: (id, action, opinion) => http.post(`/api/refusal/joint/${id}/review`, null,
    { params: { action, opinion } }),
  jointResult: (data) => http.post('/api/refusal/joint/result', data),

  issues: (status) => http.get('/api/ledger/issues', { params: { status } }),
  followups: (status) => http.get('/api/ledger/followups', { params: { status } }),
  subsidies: (status) => http.get('/api/ledger/subsidies', { params: { status } }),
  refusals: (dueOnly) => http.get('/api/ledger/refusals', { params: { dueOnly } }),
  jointComms: (status) => http.get('/api/ledger/joint-comms', { params: { status } }),

  overview: (params) => http.get('/api/stats/overview', { params }),
  trendGrade: (params) => http.get('/api/stats/trend-grade', { params }),
  bySchool: (regionId) => http.get('/api/stats/by-school', { params: { regionId } }),
  byRegion: () => http.get('/api/stats/by-region'),
  issueDist: () => http.get('/api/stats/issues'),
  stageFunnel: (params) => http.get('/api/stats/stage-funnel', { params })
}
