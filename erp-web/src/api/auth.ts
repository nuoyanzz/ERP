import request from '@/utils/request'

export interface LoginVO {
  token: string
  refreshToken?: string
  userId: number
  username: string
  nickname: string
  perms: string[]
  passwordExpired?: boolean
}

export const authApi = {
  login: (data: { username: string; password: string }) => request.post<any, LoginVO>('/auth/login', data),
  refresh: (refreshToken: string) => request.post<any, LoginVO>('/auth/refresh', { refreshToken }),
  changePassword: (oldPassword: string, newPassword: string) =>
    request.post<any, void>('/auth/change-password', { oldPassword, newPassword }),
  logout: () => request.post<any, void>('/auth/logout')
}








