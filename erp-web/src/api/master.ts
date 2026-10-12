import request from '@/utils/request'

export interface MaterialVO {
  id: number
  materialCode: string
  materialName: string
  spec?: string
  categoryId?: number
  unit?: string
  trackBatch: number
  trackSn: number
  costPrice: number
  safeStock: number
  status: number
  remark?: string
}

export interface CustomerVO {
  id: number
  customerCode: string
  customerName: string
  creditLimit: number
  currency?: string
  taxNo?: string
  contactName?: string
  contactPhone?: string
  salesDeptId?: number
  paymentDays: number
  discountRate: number
  status: number
  remark?: string
}

export interface SupplierVO {
  id: number
  supplierCode: string
  supplierName: string
  taxNo?: string
  settleType?: string
  level: number
  contactName?: string
  contactPhone?: string
  paymentDays: number
  status: number
  remark?: string
}

export interface WarehouseVO {
  id: number
  warehouseCode: string
  warehouseName: string
  whType: number
  location?: string
  allowNegative: number
  freezeStock: number
  status: number
  remark?: string
}

export const materialApi = {
  page: (params: any) =>
    request.get<any, { records: MaterialVO[]; total: number }>('/md/material/page', { params }),
  list: () => request.get<any, MaterialVO[]>('/md/material/list'),
  detail: (id: number) => request.get<any, MaterialVO>(`/md/material/${id}`),
  create: (data: any) => request.post<any, number>('/md/material', data),
  update: (data: any) => request.put<any, void>('/md/material', data),
  remove: (id: number) => request.delete<any, void>(`/md/material/${id}`)
}

export const materialCategoryApi = {
  tree: () => request.get<any, any[]>('/md/material-category/tree'),
  detail: (id: number) => request.get<any, any>(`/md/material-category/${id}`),
  create: (data: any) => request.post<any, number>('/md/material-category', data),
  update: (data: any) => request.put<any, void>('/md/material-category', data),
  remove: (id: number) => request.delete<any, void>(`/md/material-category/${id}`)
}

export const customerApi = {
  page: (params: any) =>
    request.get<any, { records: CustomerVO[]; total: number }>('/md/customer/page', { params }),
  list: () => request.get<any, CustomerVO[]>('/md/customer/list'),
  detail: (id: number) => request.get<any, CustomerVO>(`/md/customer/${id}`),
  create: (data: any) => request.post<any, number>('/md/customer', data),
  update: (data: any) => request.put<any, void>('/md/customer', data),
  remove: (id: number) => request.delete<any, void>(`/md/customer/${id}`)
}

export const supplierApi = {
  page: (params: any) =>
    request.get<any, { records: SupplierVO[]; total: number }>('/md/supplier/page', { params }),
  list: () => request.get<any, SupplierVO[]>('/md/supplier/list'),
  detail: (id: number) => request.get<any, SupplierVO>(`/md/supplier/${id}`),
  create: (data: any) => request.post<any, number>('/md/supplier', data),
  update: (data: any) => request.put<any, void>('/md/supplier', data),
  remove: (id: number) => request.delete<any, void>(`/md/supplier/${id}`)
}

export const warehouseApi = {
  page: (params: any) =>
    request.get<any, { records: WarehouseVO[]; total: number }>('/md/warehouse/page', { params }),
  list: () => request.get<any, WarehouseVO[]>('/md/warehouse/list'),
  detail: (id: number) => request.get<any, WarehouseVO>(`/md/warehouse/${id}`),
  create: (data: any) => request.post<any, number>('/md/warehouse', data),
  update: (data: any) => request.put<any, void>('/md/warehouse', data),
  remove: (id: number) => request.delete<any, void>(`/md/warehouse/${id}`)
}








