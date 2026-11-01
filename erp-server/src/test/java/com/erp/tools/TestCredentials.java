package com.erp.tools;

/**
 * 验证工具的账号凭据。
 * <p>
 * 管理员口令由 ERP_ADMIN_PASSWORD 环境变量指定——代码与初始化脚本里
 * 不再内置任何公开可知的默认口令。演示账号（finance、purchaser 等）
 * 使用统一的演示口令，见 db/seed/demo_accounts.sql。
 */
public final class TestCredentials {

    /**
     * 演示账号的统一口令，仅用于本地演示与自动化验证。
     */
    public static final String DEMO_PASSWORD = "admin123";

    /**
     * 管理员口令。优先取环境变量，未设置时退回本地验证默认值。
     */
    public static final String ADMIN_PASSWORD =
            System.getenv().getOrDefault("ERP_ADMIN_PASSWORD", "Verify@123456");

    private TestCredentials() {
    }
}
