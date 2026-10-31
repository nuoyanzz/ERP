package com.erp.security;

import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超级管理员判定。
 * <p>
 * 内置管理员角色（角色 ID = 1）不受数据范围与审批流约束。
 */
@Component
@RequiredArgsConstructor
public class SuperAdminChecker {

    /** 内置管理员角色 ID */
    public static final Long ADMIN_ROLE_ID = 1L;

    private final SysUserMapper userMapper;

    /**
     * 判断用户是否为超级管理员。
     */
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(userId);
        return roleIds != null && roleIds.contains(ADMIN_ROLE_ID);
    }

    /**
     * 判断当前登录用户是否为超级管理员。
     */
    public boolean isCurrentUserSuperAdmin() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        return loginUser != null && isSuperAdmin(loginUser.getUserId());
    }
}








