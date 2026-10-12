import request from '@/utils/request'

export interface PaOrderVO {
  id: number
  orderNo: string
  supplierId: number
  supplierName?: string
  orderDate: string
  deliveryDate?: string
  status: number
  totalAmount: number
  remark?: string
  createTime?: string
  entries?: any[]
}

export interface PaInstockVO {
  id: number
  instockNo: string
  orderId?: number
  orderNo?: string
  supplierId: number
  supplierName?: string
  warehouseId: number
  warehouseName?: string
  instockDate: string
  status: number
  voucherStatus: number
  totalAmount: number
  totalCost?: number
  taxAmount?: number
  createTime?: string
  entries?: any[]
}

export interface PaReturnVO {
  id: number
  returnNo: string
  sourceInstockId: number
  supplierId: number
  supplierName?: string
  warehouseId: number
  returnDate: string
  status: number
  totalAmount: number
  createTime?: string
  entries?: any[]
}

export const paOrderApi = {
  page: (params: any) =>
    request.get<any, { records: PaOrderVO[]; total: number }>('/pa/order/page', { params }),
  detail: (id: number) => request.get<any, PaOrderVO>(`/pa/order/${id}`),
  create: (data: any) => request.post<any, number>('/pa/order', data),
  update: (data: any) => request.put<any, void>('/pa/order', data),
  remove: (id: number) => request.delete<any, void>(`/pa/order/${id}`),
  audit: (id: number) => request.post<any, void>(`/pa/order/${id}/audit`),
  close: (id: number, closeReason?: string) =>
    request.post<any, void>(`/pa/order/${id}/close`, { closeReason })
}

export const paInstockApi = {
  page: (params: any) =>
    request.get<any, { records: PaInstockVO[]; total: number }>('/pa/instock/page', { params }),
  detail: (id: number) => request.get<any, PaInstockVO>(`/pa/instock/${id}`),
  create: (data: any) => request.post<any, number>('/pa/instock', data),
  update: (data: any) => request.put<any, void>('/pa/instock', data),
  remove: (id: number) => request.delete<any, void>(`/pa/instock/${id}`),
  audit: (id: number) => request.post<any, void>(`/pa/instock/${id}/audit`)
}

export const paReturnApi = {
  page: (params: any) =>
    request.get<any, { records: PaReturnVO[]; total: number }>('/pa/return/page', { params }),
  detail: (id: number) => request.get<any, PaReturnVO>(`/pa/return/${id}`),
  create: (data: any) => request.post<any, number>('/pa/return', data),
  update: (data: any) => request.put<any, void>('/pa/return', data),
  remove: (id: number) => request.delete<any, void>(`/pa/return/${id}`),
  audit: (id: number) => request.post<any, void>(`/pa/return/${id}/audit`)
}








