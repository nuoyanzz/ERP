package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.MenuAssignRequest;
import com.erp.system.domain.dto.RoleQueryRequest;
import com.erp.system.domain.dto.RoleSaveRequest;
import com.erp.system.domain.entity.SysRole;
import com.erp.system.domain.entity.SysRoleMenu;
import com.erp.system.domain.vo.RoleVO;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private static final Long ADMIN_ROLE_ID = 1L;

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageResult<RoleVO> pageRole(RoleQueryRequest request) {
        Page<SysRole> page = this.page(request.toPage(), Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(request.getRoleCode()), SysRole::getRoleCode, request.getRoleCode())
                .like(StringUtils.hasText(request.getRoleName()), SysRole::getRoleName, request.getRoleName())
                .eq(request.getStatus() != null, SysRole::getStatus, request.getStatus())
                .orderByDesc(SysRole::getId));
        List<RoleVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<RoleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<RoleVO> listRoles() {
        List<SysRole> roles = this.list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, 0)
                .orderByAsc(SysRole::getId));
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        RoleVO vo = toVO(role);
        vo.setMenuIds(menuMapper.selectMenuIdsByRoleId(id));
        return vo;
    }

    @Override
    public Long createRole(RoleSaveRequest request) {
        Long count = this.count(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, request.getRoleCode()));
        if (count != null && count > 0) {
            throw new BizException("角色编码已存在");
        }
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        if (role.getDataScope() == null) {
            role.setDataScope(1);
        }
        this.save(role);
        return role.getId();
    }

    @Override
    public void updateRole(RoleSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SysRole exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        Long count = this.count(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, request.getRoleCode())
                .ne(SysRole::getId, request.getId()));
        if (count != null && count > 0) {
            throw new BizException("角色编码已存在");
        }
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        this.updateById(role);
    }

    @Override
    public void deleteRoles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择要删除的角色");
        }
        if (ids.contains(ADMIN_ROLE_ID)) {
            throw new BizException("内置管理员角色不能删除");
        }
        this.removeByIds(ids);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, ids));
    }

    @Override
    public void assignMenus(Long id, MenuAssignRequest request) {
        SysRole exist = this.getById(id);
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        List<Long> menuIds = request.getMenuIds() == null ? Collections.emptyList() : request.getMenuIds();
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(id);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
    }

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}








