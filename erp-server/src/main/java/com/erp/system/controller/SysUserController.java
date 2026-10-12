package com.erp.system.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.AssignRoleRequest;
import com.erp.system.domain.dto.PasswordUpdateRequest;
import com.erp.system.domain.dto.UserQueryRequest;
import com.erp.system.domain.dto.UserSaveRequest;
import com.erp.system.domain.vo.RoleVO;
import com.erp.system.domain.vo.UserVO;
import com.erp.system.service.SysRoleService;
import com.erp.system.service.SysUserService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysRoleService roleService;

    @GetMapping("/page")
    @RequiresPermission("sys:user:query")
    @OperLog(module = "sys")
    public Result<PageResult<UserVO>> page(UserQueryRequest request) {
        return Result.ok(userService.pageUser(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    @OperLog(module = "sys")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    @PostMapping
    @RequiresPermission("sys:user:add")
    @OperLog(module = "sys")
    public Result<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.ok(userService.createUser(request));
    }

    @PutMapping
    @RequiresPermission("sys:user:edit")
    @OperLog(module = "sys")
    public Result<Void> update(@Valid @RequestBody UserSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        userService.updateUser(request);
        return Result.ok();
    }

    @DeleteMapping
    @RequiresPermission("sys:user:remove")
    @OperLog(module = "sys")
    public Result<Void> delete(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:user:remove")
    @OperLog(module = "sys")
    public Result<Void> deleteOne(@PathVariable Long id) {
        userService.deleteUsers(List.of(id));
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @RequiresPermission("sys:user:edit")
    @OperLog(module = "sys")
    public Result<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    @RequiresPermission("sys:user:edit")
    @OperLog(module = "sys")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("sys:user:edit")
    @OperLog(module = "sys")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRoles(id, request);
        return Result.ok();
    }

    @GetMapping("/roles")
    @RequiresPermission("sys:user:query")
    @OperLog(module = "sys")
    public Result<List<RoleVO>> roles() {
        return Result.ok(roleService.listRoles());
    }
}








