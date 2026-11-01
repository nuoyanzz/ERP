package com.erp.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.domain.entity.SysUserRole;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 初始管理员账号初始化。
 * <p>
 * 管理员口令不写死在代码里，也不使用任何公开可知的默认值——
 * 公开仓库里的默认口令等于把超级管理员权限送给每一个访问者。
 * 口令由部署方通过环境变量 {@code ERP_ADMIN_PASSWORD} 提供。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 初始管理员口令。未配置时留空，此时不自动建号。
     */
    @Value("${erp.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, ADMIN_USERNAME));
        if (count != null && count > 0) {
            return;
        }
        if (!StringUtils.hasText(adminPassword)) {
            log.warn("未配置 ERP_ADMIN_PASSWORD，跳过初始管理员账号创建。"
                    + "请设置该环境变量后重启，或在系统管理中手工创建管理员账号。");
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("超级管理员");
        admin.setStatus(0);
        userMapper.insert(admin);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(1L);
        userRoleMapper.insert(userRole);
        // 只提示账号名，绝不把口令写进日志
        log.info("初始管理员账号创建成功：{}，请立即登录并修改口令", ADMIN_USERNAME);
    }
}
