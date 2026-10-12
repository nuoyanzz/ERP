import request from '@/utils/request'

export interface AccountTreeVO {
  id: number
  subjectCode: string
  subjectName: string
  parentId: number
  category: number
  balanceDir: number
  assistCustomer: number
  assistSupplier: number
  assistDept: number
  assistProject: number
  currency: string | null
  status: number
  remark: string | null
  children?: AccountTreeVO[]
}

export const accountApi = {
  tree: () => request.get<any, AccountTreeVO[]>('/fi/account/tree'),
  detail: (id: number) => request.get<any, any>(`/fi/account/${id}`),
  create: (data: any) => request.post<any, number>('/fi/account', data),
  update: (data: any) => request.put<any, void>('/fi/account', data),
  remove: (id: number) => request.delete<any, void>(`/fi/account/${id}`)
}

export const periodApi = {
  list: (periodYear?: number) => request.get<any, any[]>('/fi/period/list', { params: { periodYear } }),
  detail: (id: number) => request.get<any, any>(`/fi/period/${id}`),
  generate: (periodYear: number) => request.post<any, number>('/fi/period/generate', { periodYear }),
  close: (id: number) => request.put<any, void>(`/fi/period/${id}/close`),
  open: (id: number) => request.put<any, void>(`/fi/period/${id}/open`)
}

export const voucherApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/fi/voucher/page', { params }),
  detail: (id: number) => request.get<any, any>(`/fi/voucher/${id}`),
  create: (data: any) => request.post<any, number>('/fi/voucher', data),
  update: (data: any) => request.put<any, void>('/fi/voucher', data),
  remove: (id: number) => request.delete<any, void>(`/fi/voucher/${id}`),
  submit: (id: number) => request.post<any, void>(`/fi/voucher/${id}/submit`),
  audit: (id: number) => request.post<any, void>(`/fi/voucher/${id}/audit`),
  unaudit: (id: number) => request.post<any, void>(`/fi/voucher/${id}/unaudit`),
  post: (id: number) => request.post<any, void>(`/fi/voucher/${id}/post`),
  unpost: (id: number) => request.post<any, void>(`/fi/voucher/${id}/unpost`),
  voidVoucher: (id: number, reason: string) =>
    request.post<any, void>(`/fi/voucher/${id}/void`, null, { params: { reason } }),
  redFlush: (id: number) => request.post<any, number>(`/fi/voucher/${id}/red-flush`)
}

export const balanceApi = {
  account: (period: string) => request.get<any, any[]>('/fi/balance/account', { params: { period } })
}

export const openingApi = {
  list: (period: string) => request.get<any, any[]>('/fi/opening/list', { params: { period } }),
  save: (data: any) => request.post<any, number>('/fi/opening', data),
  remove: (id: number) => request.delete<any, void>(`/fi/opening/${id}`),
  trialBalance: (period: string) => request.get<any, any>('/fi/opening/trial-balance', { params: { period } })
}

export const carryForwardApi = {
  profitAndLoss: (period: string) =>
    request.post<any, number>('/fi/carry-forward/profit-and-loss', null, { params: { period } }),
  yearEnd: (period: string) =>
    request.post<any, number>('/fi/carry-forward/year-end', null, { params: { period } }),
  reverse: (period: string, carryType: number) =>
    request.post<any, void>('/fi/carry-forward/reverse', null, { params: { period, carryType } })
}

/**
 * 期末结转接口别名，与历史页面保持一致。
 */
export const carryApi = carryForwardApi

export const reportApi = {
  balanceSheet: (period: string) => request.get<any, any>('/fi/report/balance-sheet', { params: { period } }),
  incomeStatement: (period: string) => request.get<any, any>('/fi/report/income-statement', { params: { period } }),
  cashFlowStatement: (period: string) =>
    request.get<any, any>('/fi/report/cash-flow-statement', { params: { period } })
}








