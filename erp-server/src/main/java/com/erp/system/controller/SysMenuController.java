package com.erp.system.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.MenuSaveRequest;
import com.erp.system.domain.vo.MenuTreeVO;
import com.erp.system.service.SysMenuService;
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
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @GetMapping("/tree")
    @RequiresPermission("sys:menu:query")
    @OperLog(module = "sys")
    public Result<List<MenuTreeVO>> tree() {
        return Result.ok(menuService.treeMenus());
    }

    @GetMapping("/my")
    @RequiresPermission("sys:menu:query")
    @OperLog(module = "sys")
    public Result<List<MenuTreeVO>> myMenus() {
        return Result.ok(menuService.myMenus());
    }

    @GetMapping("/my-perms")
    @RequiresPermission("sys:menu:query")
    @OperLog(module = "sys")
    public Result<List<String>> myPerms() {
        return Result.ok(menuService.myPerms());
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:menu:query")
    @OperLog(module = "sys")
    public Result<MenuTreeVO> detail(@PathVariable Long id) {
        return Result.ok(menuService.getMenuDetail(id));
    }

    @PostMapping
    @RequiresPermission("sys:menu:add")
    @OperLog(module = "sys")
    public Result<Long> create(@Valid @RequestBody MenuSaveRequest request) {
        return Result.ok(menuService.createMenu(request));
    }

    @PutMapping
    @RequiresPermission("sys:menu:edit")
    @OperLog(module = "sys")
    public Result<Void> update(@Valid @RequestBody MenuSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        menuService.updateMenu(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:menu:remove")
    @OperLog(module = "sys")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.ok();
    }
}








