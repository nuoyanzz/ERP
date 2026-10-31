package com.erp.security.service;

import com.erp.common.exception.BizException;
import com.erp.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码策略校验器。
 * <p>
 * 集中管理密码强度规则，避免各处校验口径不一致。
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    /** 密码必须同时包含字母与数字 */
    private static final Pattern COMPLEXITY_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    /** 常见弱密码，禁止使用 */
    private static final String[] WEAK_PASSWORDS = {
            "123456", "admin123", "password", "password123", "qwerty", "111111", "888888", "admin"
    };

    private final JwtProperties jwtProperties;

    /**
     * 校验密码强度，不满足时抛出业务异常。
     */
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException("密码不能为空");
        }
        if (password.length() < jwtProperties.getPasswordMinLength()) {
            throw new BizException("密码长度不能少于 " + jwtProperties.getPasswordMinLength() + " 位");
        }
        if (password.length() > 64) {
            throw new BizException("密码长度不能超过 64 位");
        }
        if (jwtProperties.isPasswordRequireComplexity() && !COMPLEXITY_PATTERN.matcher(password).matches()) {
            throw new BizException("密码必须同时包含字母和数字");
        }
        String lower = password.toLowerCase();
        for (String weak : WEAK_PASSWORDS) {
            if (lower.contains(weak)) {
                throw new BizException("密码过于简单，包含常见弱口令");
            }
        }
    }
}








