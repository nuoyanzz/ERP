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









export interface AgingVO {
  supplierId?: number
  customerId?: number
  name: string
  notDue: number
  within30: number
  within60: number
  within90: number
  over90: number
}

export const apAgingApi = {
  aging: (asOfDate?: string) => request.get<any, AgingVO[]>('/ap/aging', { params: { asOfDate } })
}

export interface BadDebtVO {
  id: number
  recordNo: string
  period: string
  customerId: number
  customerName?: string
  amount: number
  reason?: string
  status: number
  auditBy?: number
  auditTime?: string
  remark?: string
}

export const badDebtApi = {
  page: (params: any) =>
    request.get<any, { records: BadDebtVO[]; total: number }>('/ar/baddebt/page', { params }),
  detail: (id: number) => request.get<any, BadDebtVO>(`/ar/baddebt/${id}`),
  create: (data: any) => request.post<any, number>('/ar/baddebt', data),
  update: (data: any) => request.put<any, void>('/ar/baddebt', data),
  remove: (id: number) => request.delete<any, void>(`/ar/baddebt/${id}`),
  audit: (id: number) => request.post<any, void>(`/ar/baddebt/${id}/audit`)
}

export interface ReconVO {
  customerId: number
  customerName?: string
  receivableBalance: number
  ledgerBalance: number
  difference: number
  checked: boolean
}

export const reconApi = {
  recon: (customerId: number) => request.get<any, ReconVO[]>('/ar/recon', { params: { customerId } })
}
