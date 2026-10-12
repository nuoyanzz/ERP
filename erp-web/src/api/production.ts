import request from '@/utils/request'

export const bomApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/bom/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/bom/${id}`),
  create: (data: any) => request.post<any, number>('/pm/bom', data),
  update: (data: any) => request.put<any, void>('/pm/bom', data),
  remove: (id: number) => request.delete<any, void>(`/pm/bom/${id}`)
}

export const moApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/mo/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/mo/${id}`),
  create: (data: any) => request.post<any, number>('/pm/mo', data),
  update: (data: any) => request.put<any, void>('/pm/mo', data),
  remove: (id: number) => request.delete<any, void>(`/pm/mo/${id}`),
  audit: (id: number) => request.post<any, void>(`/pm/mo/${id}/audit`)
}

export const pickApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/pick/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/pick/${id}`),
  create: (data: any) => request.post<any, number>('/pm/pick', data),
  update: (data: any) => request.put<any, void>('/pm/pick', data),
  remove: (id: number) => request.delete<any, void>(`/pm/pick/${id}`),
  audit: (id: number) => request.post<any, void>(`/pm/pick/${id}/audit`)
}

export const finishApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/finish/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/finish/${id}`),
  create: (data: any) => request.post<any, number>('/pm/finish', data),
  update: (data: any) => request.put<any, void>('/pm/finish', data),
  remove: (id: number) => request.delete<any, void>(`/pm/finish/${id}`),
  audit: (id: number) => request.post<any, void>(`/pm/finish/${id}/audit`)
}

export const mrpApi = {
  run: () => request.post<any, any>('/pm/mrp/run'),
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/mrp/page', { params }),
  latestRunNo: () => request.get<any, string>('/pm/mrp/latest-run-no'),
  releasePurchase: (runNo: string) => request.post<any, any>('/pm/mrp/release-purchase', null, { params: { runNo } }),
  releaseProduction: (runNo: string) => request.post<any, any>('/pm/mrp/release-production', null, { params: { runNo } })
}

export const routingApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/routing/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/routing/${id}`),
  workCenters: () => request.get<any, any[]>('/pm/routing/work-centers'),
  create: (data: any) => request.post<any, number>('/pm/routing', data),
  update: (data: any) => request.put<any, void>('/pm/routing', data),
  remove: (id: number) => request.delete<any, void>(`/pm/routing/${id}`),
  saveOperations: (id: number, operations: any[]) =>
    request.post<any, void>(`/pm/routing/${id}/operations`, operations),
  updateOperation: (id: number, operationId: number, data: any) =>
    request.put<any, void>(`/pm/routing/${id}/operations/${operationId}`, data),
  removeOperation: (id: number, operationId: number) =>
    request.delete<any, void>(`/pm/routing/${id}/operations/${operationId}`)
}

export const reportWorkApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/report/page', { params }),
  detail: (id: number) => request.get<any, any>(`/pm/report/${id}`),
  create: (data: any) => request.post<any, number>('/pm/report', data),
  update: (data: any) => request.put<any, void>('/pm/report', data),
  remove: (id: number) => request.delete<any, void>(`/pm/report/${id}`),
  audit: (id: number) => request.post<any, void>(`/pm/report/${id}/audit`)
}

export const costApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/pm/cost/page', { params }),
  detail: (moId: number) => request.get<any, any>(`/pm/cost/detail/${moId}`),
  detailPage: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/pm/cost/detail-page', { params }),
  list: (params: any) => request.get<any, any[]>('/pm/cost/list', { params })
}








