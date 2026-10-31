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
    /**
     * 常见弱密码，禁止使用。
     * <p>
     * 按「去掉非字母数字后的完全匹配」判断，而不是子串包含——
     * 否则 admin123 会被 admin 命中、mypassword1 会被 password 命中，
     * 正常密码被误杀。
     */
    private static final String[] WEAK_PASSWORDS = {
            "123456", "admin", "password", "qwerty", "111111", "888888", "abc123", "654321"
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
        // 归一化后与弱口令表完全匹配才算命中
        String normalized = password.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (String weak : WEAK_PASSWORDS) {
            if (normalized.equals(weak)) {
                throw new BizException("密码过于简单，禁止使用常见弱口令");
            }
        }
    }
}








