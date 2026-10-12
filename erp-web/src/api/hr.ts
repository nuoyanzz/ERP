import request from '@/utils/request'

export const employeeApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/hr/employee/page', { params }),
  detail: (id: number) => request.get<any, any>(`/hr/employee/${id}`),
  create: (data: any) => request.post<any, number>('/hr/employee', data),
  update: (data: any) => request.put<any, void>('/hr/employee', data),
  remove: (id: number) => request.delete<any, void>(`/hr/employee/${id}`)
}

export const deptApi = {
  tree: () => request.get<any, any[]>('/hr/dept/tree'),
  detail: (id: number) => request.get<any, any>(`/hr/dept/${id}`),
  create: (data: any) => request.post<any, number>('/hr/dept', data),
  update: (data: any) => request.put<any, void>('/hr/dept', data),
  remove: (id: number) => request.delete<any, void>(`/hr/dept/${id}`)
}

export const orgApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/hr/org/page', { params }),
  list: () => request.get<any, any[]>('/hr/org/list'),
  detail: (id: number) => request.get<any, any>(`/hr/org/${id}`),
  create: (data: any) => request.post<any, number>('/hr/org', data),
  update: (data: any) => request.put<any, void>('/hr/org', data),
  remove: (id: number) => request.delete<any, void>(`/hr/org/${id}`)
}

export const salaryApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/hr/salary/page', { params }),
  detail: (id: number) => request.get<any, any>(`/hr/salary/${id}`),
  create: (data: any) => request.post<any, number>('/hr/salary', data),
  update: (data: any) => request.put<any, void>('/hr/salary', data),
  remove: (id: number) => request.delete<any, void>(`/hr/salary/${id}`),
  audit: (id: number) => request.post<any, void>(`/hr/salary/${id}/audit`)
}

export const attendanceApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/hr/attendance/page', { params }),
  create: (data: any) => request.post<any, number>('/hr/attendance', data),
  update: (data: any) => request.put<any, void>('/hr/attendance', data),
  remove: (id: number) => request.delete<any, void>(`/hr/attendance/${id}`)
}

export const socialPolicyApi = {
  list: () => request.get<any, any[]>('/hr/policy/list'),
  create: (data: any) => request.post<any, number>('/hr/policy', data),
  update: (data: any) => request.put<any, void>('/hr/policy', data),
  remove: (id: number) => request.delete<any, void>(`/hr/policy/${id}`)
}








