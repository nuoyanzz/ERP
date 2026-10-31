package com.erp.security.datascope;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.hr.domain.entity.HrDept;
import com.erp.hr.mapper.HrDeptMapper;
import com.erp.security.LoginUser;
import com.erp.security.SecurityUtils;
import com.erp.system.domain.entity.SysRole;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.domain.entity.SysUserRole;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysRoleDeptMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限解析器。
 * <p>
 * 取用户所有角色中最宽松的数据范围（全部 &gt; 自定义 &gt; 本部门及以下 &gt; 本部门 &gt; 仅本人），
 * 与主流 ERP 的「角色取并集」语义一致。
 */
@Component
@RequiredArgsConstructor
public class DataScopeResolver {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final HrDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    /**
     * 解析当前登录用户的数据范围。
     */
    public DataScopeContext.DataScopeInfo resolveCurrent() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }
        return resolve(loginUser.getUserId());
    }

    /**
     * 解析指定用户的数据范围。
     */
    public DataScopeContext.DataScopeInfo resolve(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();

        DataScopeContext.DataScopeInfo info = new DataScopeContext.DataScopeInfo();
        info.setUserId(userId);
        info.setDeptId(user.getDeptId());

        if (roleIds.isEmpty()) {
            // 无角色时只能看本人数据
            info.setScope(DataScope.SELF);
            return info;
        }

        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        DataScope scope = DataScope.SELF;
        for (SysRole role : roles) {
            DataScope roleScope = DataScope.of(role.getDataScope());
            if (isWider(scope, roleScope)) {
                scope = roleScope;
            }
        }
        info.setScope(scope);

        if (scope == DataScope.DEPT && user.getDeptId() != null) {
            info.setDeptIds(new ArrayList<>(Collections.singletonList(user.getDeptId())));
        } else if (scope == DataScope.DEPT_AND_CHILD && user.getDeptId() != null) {
            info.setDeptIds(collectDeptAndChildren(user.getDeptId()));
        } else if (scope == DataScope.CUSTOM) {
            info.setDeptIds(collectCustomDepts(roleIds));
        }
        return info;
    }

    /**
     * 判断 candidate 是否比 current 更宽松。
     */
    private boolean isWider(DataScope current, DataScope candidate) {
        return weight(candidate) > weight(current);
    }

    private int weight(DataScope scope) {
        return switch (scope) {
            case ALL -> 100;
            case CUSTOM -> 80;
            case DEPT_AND_CHILD -> 60;
            case DEPT -> 40;
            case SELF -> 20;
        };
    }

    /**
     * 收集部门及其所有子部门。
     */
    private List<Long> collectDeptAndChildren(Long deptId) {
        List<HrDept> all = deptMapper.selectList(Wrappers.<HrDept>lambdaQuery()
                .select(HrDept::getId, HrDept::getParentId));
        Set<Long> result = new LinkedHashSet<>();
        result.add(deptId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (HrDept dept : all) {
                Long parentId = dept.getParentId();
                if (parentId != null && result.contains(parentId) && result.add(dept.getId())) {
                    changed = true;
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 收集自定义数据范围角色的部门集合。
     */
    private List<Long> collectCustomDepts(List<Long> roleIds) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long deptId : roleDeptMapper.selectDeptIdsByRoleIds(roleIds)) {
            result.add(deptId);
        }
        return new ArrayList<>(result);
    }
}








