package com.erp.system.service;

import com.erp.common.api.PageResult;
import com.erp.system.domain.dto.AssignRoleRequest;
import com.erp.system.domain.dto.PasswordUpdateRequest;
import com.erp.system.domain.dto.UserQueryRequest;
import com.erp.system.domain.dto.UserSaveRequest;
import com.erp.system.domain.vo.UserVO;

import java.util.List;

public interface SysUserService {

    PageResult<UserVO> pageUser(UserQueryRequest request);

    UserVO getUserDetail(Long id);

    Long createUser(UserSaveRequest request);

    void updateUser(UserSaveRequest request);

    void deleteUsers(List<Long> ids);

    void updatePassword(Long id, PasswordUpdateRequest request);

    void resetPassword(Long id);

    void assignRoles(Long id, AssignRoleRequest request);
}








