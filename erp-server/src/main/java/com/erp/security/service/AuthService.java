package com.erp.security.service;

import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginVO;
import com.erp.security.dto.RefreshTokenRequest;

public interface AuthService {

    LoginVO login(LoginRequest request);

    /**
     * 用刷新令牌换取新的访问令牌。
     */
    LoginVO refresh(RefreshTokenRequest request);

    /**
     * 修改自己的密码。
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}








