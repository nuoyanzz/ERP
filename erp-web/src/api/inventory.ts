import request from '@/utils/request'

export const stockApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/ic/stock/page', { params })
}

export const transactionApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ic/transaction/page', { params })
}

export const transferApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ic/transfer/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ic/transfer/${id}`),
  create: (data: any) => request.post<any, number>('/ic/transfer', data),
  update: (data: any) => request.put<any, void>('/ic/transfer', data),
  remove: (id: number) => request.delete<any, void>(`/ic/transfer/${id}`),
  audit: (id: number) => request.post<any, void>(`/ic/transfer/${id}/audit`)
}

export const checkApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/ic/check/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ic/check/${id}`),
  create: (data: any) => request.post<any, number>('/ic/check', data),
  update: (data: any) => request.put<any, void>('/ic/check', data),
  remove: (id: number) => request.delete<any, void>(`/ic/check/${id}`),
  audit: (id: number) => request.post<any, void>(`/ic/check/${id}/audit`)
}








