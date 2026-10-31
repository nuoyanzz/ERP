import request from '@/utils/request'

export interface MenuTreeVO {
  id: number
  parentId: number
  menuName: string
  menuType: number
  perms: string | null
  path: string | null
  component: string | null
  icon: string | null
  sortNo: number
  visible: number
  children?: MenuTreeVO[]
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  deptId?: number
  status: number
  remark?: string
  createTime?: string
  roleIds?: number[]
}

export interface RoleVO {
  id: number
  roleCode: string
  roleName: string
  dataScope: number
  status: number
  remark?: string
  menuIds?: number[]
}

export const menuApi = {
  tree: () => request.get<any, MenuTreeVO[]>('/sys/menu/tree'),
  my: () => request.get<any, MenuTreeVO[]>('/sys/menu/my'),
  myPerms: () => request.get<any, string[]>('/sys/menu/my-perms'),
  detail: (id: number) => request.get<any, MenuTreeVO>(`/sys/menu/${id}`),
  create: (data: any) => request.post<any, number>('/sys/menu', data),
  update: (data: any) => request.put<any, void>('/sys/menu', data),
  remove: (id: number) => request.delete<any, void>(`/sys/menu/${id}`)
}

export const userApi = {
  page: (params: any) => request.get<any, { records: UserVO[]; total: number }>('/sys/user/page', { params }),
  detail: (id: number) => request.get<any, UserVO>(`/sys/user/${id}`),
  create: (data: any) => request.post<any, number>('/sys/user', data),
  update: (data: any) => request.put<any, void>('/sys/user', data),
  remove: (id: number) => request.delete<any, void>(`/sys/user/${id}`),
  changePassword: (id: number, oldPassword: string, newPassword: string) =>
    request.put<any, void>(`/sys/user/${id}/password`, { oldPassword, newPassword }),
  resetPassword: (id: number) => request.put<any, void>(`/sys/user/${id}/reset-password`),
  assignRoles: (id: number, roleIds: number[]) =>
    request.put<any, void>(`/sys/user/${id}/roles`, roleIds),
  availableRoles: () => request.get<any, RoleVO[]>('/sys/user/roles')
}

export const roleApi = {
  page: (params: any) => request.get<any, { records: RoleVO[]; total: number }>('/sys/role/page', { params }),
  list: () => request.get<any, RoleVO[]>('/sys/role/list'),
  detail: (id: number) => request.get<any, RoleVO>(`/sys/role/${id}`),
  create: (data: any) => request.post<any, number>('/sys/role', data),
  update: (data: any) => request.put<any, void>('/sys/role', data),
  remove: (id: number) => request.delete<any, void>(`/sys/role/${id}`),
  assignMenus: (id: number, menuIds: number[]) =>
    request.put<any, void>(`/sys/role/${id}/menus`, menuIds)
}

export const dictApi = {
  types: () => request.get<any, any[]>('/system/dict/types'),
  data: (dictType: string) => request.get<any, any[]>(`/system/dict/data/${dictType}`),
  refresh: () => request.post<any, void>('/system/dict/refresh')
}

export const operLogApi = {
  page: (params: any) => request.get<any, { records: any[]; total: number }>('/sys/log/page', { params })
}









export interface ParamVO {
  id: number
  paramKey: string
  paramValue?: string
  paramName?: string
  paramType: string
  status: number
  remark?: string
}

export const paramApi = {
  page: (params: any) =>
    request.get<any, { records: ParamVO[]; total: number }>('/sys/param/page', { params }),
  list: () => request.get<any, ParamVO[]>('/sys/param/list'),
  detail: (id: number) => request.get<any, ParamVO>(`/sys/param/${id}`),
  create: (data: any) => request.post<any, number>('/sys/param', data),
  update: (data: any) => request.put<any, void>('/sys/param', data),
  remove: (id: number) => request.delete<any, void>(`/sys/param/${id}`)
}
