import request from '@/utils/request'

export interface TrendPointVO {
  label: string
  amount: number
}

export interface RankItemVO {
  name: string
  amount: number
  extra: string
}

export interface TodoItemVO {
  title: string
  type: string
  count: number
  route: string
}

export interface DashboardVO {
  periodLabel: string
  salesAmount: number
  purchaseAmount: number
  stockAmount: number
  receivableAmount: number
  payableAmount: number
  pendingAuditCount: number
  todoTaskCount: number
  stockWarningCount: number
  overdueReceivableCount: number
  wipMoCount: number
  salesTrend: TrendPointVO[]
  stockTop: RankItemVO[]
  overdueTop: RankItemVO[]
  todoItems: TodoItemVO[]
}

export const dashboardApi = {
  overview: () => request.get<any, DashboardVO>('/bi/dashboard/overview')
}








