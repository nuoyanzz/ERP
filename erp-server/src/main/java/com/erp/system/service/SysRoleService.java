package com.erp.system.service;

import com.erp.common.api.PageResult;
import com.erp.system.domain.dto.MenuAssignRequest;
import com.erp.system.domain.dto.RoleQueryRequest;
import com.erp.system.domain.dto.RoleSaveRequest;
import com.erp.system.domain.vo.RoleVO;

import java.util.List;

public interface SysRoleService {

    PageResult<RoleVO> pageRole(RoleQueryRequest request);

    List<RoleVO> listRoles();

    RoleVO getRoleDetail(Long id);

    Long createRole(RoleSaveRequest request);

    void updateRole(RoleSaveRequest request);

    void deleteRoles(List<Long> ids);

    void assignMenus(Long id, MenuAssignRequest request);
}








