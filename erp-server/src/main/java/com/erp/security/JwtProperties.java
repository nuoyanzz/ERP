package com.erp.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
    /**
     * 签名密钥。不设默认值——公开仓库里的固定密钥等于任何人都能伪造令牌。
     * 生产环境通过 JWT_SECRET 环境变量注入。
     */
    private String secret = "";
    /** 访问令牌有效期（小时） */
    private long expireHours = 24;
    /** 刷新令牌有效期（天），应大于访问令牌 */
    private long refreshExpireDays = 7;

    /** 登录失败达到该次数后锁定账号 */
    private int maxLoginFailCount = 5;
    /** 锁定时长（分钟） */
    private int lockMinutes = 15;

    /** 密码最小长度 */
    private int passwordMinLength = 8;
    /** 密码有效期（天），0 表示不限制 */
    private int passwordExpireDays = 90;
    /** 是否要求密码包含字母与数字 */
    private boolean passwordRequireComplexity = true;
}








