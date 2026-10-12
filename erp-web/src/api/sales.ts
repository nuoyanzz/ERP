import request from '@/utils/request'

export interface SaOrderVO {
  id: number
  orderNo: string
  customerId: number
  customerName?: string
  orderDate: string
  deliveryDate?: string
  status: number
  totalAmount: number
  remark?: string
  createTime?: string
  entries?: any[]
}

export interface SaOutstockVO {
  id: number
  outstockNo: string
  orderId?: number
  orderNo?: string
  customerId: number
  customerName?: string
  warehouseId: number
  warehouseName?: string
  outstockDate: string
  status: number
  voucherStatus: number
  totalAmount: number
  totalCost?: number
  taxAmount?: number
  createTime?: string
  entries?: any[]
}

export interface SaReturnVO {
  id: number
  returnNo: string
  sourceOutstockId: number
  customerId: number
  customerName?: string
  warehouseId: number
  returnDate: string
  status: number
  totalAmount: number
  createTime?: string
  entries?: any[]
}

export const saOrderApi = {
  page: (params: any) =>
    request.get<any, { records: SaOrderVO[]; total: number }>('/sa/order/page', { params }),
  detail: (id: number) => request.get<any, SaOrderVO>(`/sa/order/${id}`),
  create: (data: any) => request.post<any, number>('/sa/order', data),
  update: (data: any) => request.put<any, void>('/sa/order', data),
  remove: (id: number) => request.delete<any, void>(`/sa/order/${id}`),
  audit: (id: number) => request.post<any, void>(`/sa/order/${id}/audit`),
  close: (id: number, closeReason?: string) =>
    request.post<any, void>(`/sa/order/${id}/close`, { closeReason })
}

export const saOutstockApi = {
  page: (params: any) =>
    request.get<any, { records: SaOutstockVO[]; total: number }>('/sa/outstock/page', { params }),
  detail: (id: number) => request.get<any, SaOutstockVO>(`/sa/outstock/${id}`),
  create: (data: any) => request.post<any, number>('/sa/outstock', data),
  update: (data: any) => request.put<any, void>('/sa/outstock', data),
  remove: (id: number) => request.delete<any, void>(`/sa/outstock/${id}`),
  audit: (id: number) => request.post<any, void>(`/sa/outstock/${id}/audit`)
}

export const saReturnApi = {
  page: (params: any) =>
    request.get<any, { records: SaReturnVO[]; total: number }>('/sa/return/page', { params }),
  detail: (id: number) => request.get<any, SaReturnVO>(`/sa/return/${id}`),
  create: (data: any) => request.post<any, number>('/sa/return', data),
  update: (data: any) => request.put<any, void>('/sa/return', data),
  remove: (id: number) => request.delete<any, void>(`/sa/return/${id}`),
  audit: (id: number) => request.post<any, void>(`/sa/return/${id}/audit`)
}








