package com.erp.wf.engine;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.domain.entity.SysUserRole;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 组织与人事数据查询实现。
 */
@Component
@RequiredArgsConstructor
public class WfDirectoryImpl implements WfDirectory {

    /** 部门负责人在员工表中的职级关键字，命中优先级从高到低 */
    private static final String[] LEADER_KEYWORDS = {"部门负责人", "负责人", "总监", "总经理", "经理", "主管", "厂长", "主任"};

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final HrEmployeeMapper employeeMapper;

    @Override
    public List<Long> userIdsByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getRoleId, roleIds));
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (SysUserRole userRole : userRoles) {
            userIds.add(userRole.getUserId());
        }
        return new ArrayList<>(userIds);
    }

    @Override
    public List<Long> filterEnabledUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getStatus, 0));
        Set<Long> valid = new LinkedHashSet<>();
        for (SysUser user : users) {
            valid.add(user.getId());
        }
        List<Long> result = new ArrayList<>();
        for (Long userId : userIds) {
            if (valid.contains(userId)) {
                result.add(userId);
            }
        }
        return result;
    }

    @Override
    public Long deptIdOfUser(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getDeptId();
    }

    @Override
    public String nicknameOfUser(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }

    @Override
    public List<Long> deptLeaderUserIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<HrEmployee> employees = employeeMapper.selectList(Wrappers.<HrEmployee>lambdaQuery()
                .eq(HrEmployee::getDeptId, deptId)
                .eq(HrEmployee::getStatus, HrEmployee.STATUS_ACTIVE));
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }
        List<SysUser> deptUsers = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, 0));
        if (deptUsers == null || deptUsers.isEmpty()) {
            return List.of();
        }
        for (String keyword : LEADER_KEYWORDS) {
            List<Long> matched = new ArrayList<>();
            for (HrEmployee employee : employees) {
                String position = employee.getPosition();
                if (position == null || !position.contains(keyword)) {
                    continue;
                }
                for (SysUser user : deptUsers) {
                    if (user.getNickname() != null && user.getNickname().equals(employee.getEmployeeName())) {
                        matched.add(user.getId());
                        break;
                    }
                }
            }
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        return List.of();
    }

    @Override
    public List<Long> enabledUserIdsOfDept(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<SysUser> deptUsers = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, 0)
                .orderByAsc(SysUser::getId));
        if (deptUsers == null || deptUsers.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = new ArrayList<>();
        for (SysUser user : deptUsers) {
            userIds.add(user.getId());
        }
        return userIds;
    }
}








