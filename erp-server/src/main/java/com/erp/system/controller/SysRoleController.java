package com.erp.system.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.MenuAssignRequest;
import com.erp.system.domain.dto.RoleQueryRequest;
import com.erp.system.domain.dto.RoleSaveRequest;
import com.erp.system.domain.vo.RoleVO;
import com.erp.system.service.SysRoleService;
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
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/page")
    @RequiresPermission("sys:role:query")
    @OperLog(module = "sys")
    public Result<PageResult<RoleVO>> page(RoleQueryRequest request) {
        return Result.ok(roleService.pageRole(request));
    }

    @GetMapping("/list")
    @RequiresPermission("sys:role:query")
    @OperLog(module = "sys")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.listRoles());
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:role:query")
    @OperLog(module = "sys")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(roleService.getRoleDetail(id));
    }

    @PostMapping
    @RequiresPermission("sys:role:add")
    @OperLog(module = "sys")
    public Result<Long> create(@Valid @RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.createRole(request));
    }

    @PutMapping
    @RequiresPermission("sys:role:edit")
    @OperLog(module = "sys")
    public Result<Void> update(@Valid @RequestBody RoleSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        roleService.updateRole(request);
        return Result.ok();
    }

    @DeleteMapping
    @RequiresPermission("sys:role:remove")
    @OperLog(module = "sys")
    public Result<Void> delete(@RequestBody List<Long> ids) {
        roleService.deleteRoles(ids);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:role:remove")
    @OperLog(module = "sys")
    public Result<Void> deleteOne(@PathVariable Long id) {
        roleService.deleteRoles(List.of(id));
        return Result.ok();
    }

    @PutMapping("/{id}/menus")
    @RequiresPermission("sys:role:edit")
    @OperLog(module = "sys")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody MenuAssignRequest request) {
        roleService.assignMenus(id, request);
        return Result.ok();
    }
}








