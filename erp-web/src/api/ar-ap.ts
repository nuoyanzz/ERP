import request from '@/utils/request'

export const arReceivableApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ar/receivable/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ar/receivable/${id}`),
  aging: (asOfDate?: string) => request.get<any, any[]>('/ar/receivable/aging', { params: { asOfDate } })
}

export const arReceiptApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ar/receipt/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ar/receipt/${id}`),
  create: (data: any) => request.post<any, number>('/ar/receipt', data),
  update: (data: any) => request.put<any, void>('/ar/receipt', data),
  remove: (id: number) => request.delete<any, void>(`/ar/receipt/${id}`),
  audit: (id: number) => request.post<any, void>(`/ar/receipt/${id}/audit`)
}

export const apPayableApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ap/payable/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ap/payable/${id}`)
}

export const apPaymentApi = {
  page: (params: any) =>
    request.get<any, { records: any[]; total: number }>('/ap/payment/page', { params }),
  detail: (id: number) => request.get<any, any>(`/ap/payment/${id}`),
  create: (data: any) => request.post<any, number>('/ap/payment', data),
  update: (data: any) => request.put<any, void>('/ap/payment', data),
  remove: (id: number) => request.delete<any, void>(`/ap/payment/${id}`),
  audit: (id: number) => request.post<any, void>(`/ap/payment/${id}/audit`)
}








