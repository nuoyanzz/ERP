import request from '@/utils/request'

export const definitionApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/wf/definition/page', { params }),
  detail: (id: number) => request.get<any, any>(`/wf/definition/${id}`),
  byBizType: (bizType: string) => request.get<any, any[]>(`/wf/definition/biz-type/${bizType}`),
  publishedByBizType: (bizType: string) =>
    request.get<any, any>(`/wf/definition/biz-type/${bizType}/published`),
  create: (data: any) => request.post<any, number>('/wf/definition', data),
  update: (data: any) => request.put<any, void>('/wf/definition', data),
  remove: (id: number) => request.delete<any, void>(`/wf/definition/${id}`),
  publish: (id: number) => request.post<any, void>(`/wf/definition/${id}/publish`),
  disable: (id: number) => request.post<any, void>(`/wf/definition/${id}/disable`)
}

export const instanceApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/wf/instance/page', { params }),
  detail: (id: number) => request.get<any, any>(`/wf/instance/${id}`),
  start: (data: any) => request.post<any, number>('/wf/instance/start', data),
  cancel: (id: number) => request.post<any, void>(`/wf/instance/${id}/cancel`),
  withdraw: (id: number) => request.post<any, void>(`/wf/instance/${id}/withdraw`)
}

export const taskApi = {
  todo: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/wf/task/todo', { params }),
  done: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/wf/task/done', { params }),
  byInstance: (instanceId: number) => request.get<any, any[]>(`/wf/task/instance/${instanceId}`),
  approve: (id: number, comment?: string) =>
    request.post<any, void>(`/wf/task/${id}/approve`, { comment }),
  reject: (id: number, comment?: string) => request.post<any, void>(`/wf/task/${id}/reject`, { comment }),
  rollback: (id: number, comment?: string) => request.post<any, void>(`/wf/task/${id}/rollback`, { comment }),
  transfer: (id: number, assigneeId: number, comment?: string) =>
    request.post<any, void>(`/wf/task/${id}/transfer`, { assigneeId, comment }),
  countersign: (id: number, assigneeId: number, mode: number, comment?: string) =>
    request.post<any, void>(`/wf/task/${id}/countersign`, { assigneeId, mode, comment }),
  urge: (id: number) => request.post<any, void>(`/wf/task/${id}/urge`)
}








