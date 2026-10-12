package com.erp.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.domain.entity.SysUserRole;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, ADMIN_USERNAME));
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setNickname("超级管理员");
        admin.setStatus(0);
        userMapper.insert(admin);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(1L);
        userRoleMapper.insert(userRole);
        log.info("初始化管理员账号成功：{} / {}", ADMIN_USERNAME, ADMIN_PASSWORD);
    }
}








