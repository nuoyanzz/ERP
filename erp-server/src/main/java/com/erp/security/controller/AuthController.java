package com.erp.security.controller;

import com.erp.common.api.Result;
import com.erp.security.SecurityUtils;
import com.erp.security.dto.ChangePasswordRequest;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginVO;
import com.erp.security.dto.RefreshTokenRequest;
import com.erp.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(),
                request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    /**
     * 退出登录。
     * <p>
     * 无状态 JWT 无法服务端主动失效，客户端丢弃令牌即可；
     * 保留该端点是为了让前端有统一的登出入口，便于后续接入令牌黑名单。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}








