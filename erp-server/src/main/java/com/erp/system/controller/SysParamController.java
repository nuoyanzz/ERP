package com.erp.system.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import com.erp.system.domain.dto.SysParamQueryRequest;
import com.erp.system.domain.dto.SysParamSaveRequest;
import com.erp.system.domain.vo.SysParamVO;
import com.erp.system.service.SysParamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统参数管理。
 * <p>
 * 参数的增删改查需要 {@code sys:param:*} 权限；
 * 但 {@link #value(String)} 是业务代码高频读取的入口，任何已登录用户均可调用。
 */
@RestController
@RequestMapping("/sys/param")
@RequiredArgsConstructor
public class SysParamController {

    private final SysParamService paramService;

    @GetMapping("/page")
    @RequiresPermission("sys:param:query")
    @OperLog(module = "sys")
    public Result<PageResult<SysParamVO>> page(SysParamQueryRequest request) {
        return Result.ok(paramService.pageParam(request));
    }

    /**
     * 列出全部启用中的参数，供前端下拉框与配置页使用。
     */
    @GetMapping("/list")
    @RequiresPermission("sys:param:query")
    @OperLog(module = "sys")
    public Result<List<SysParamVO>> list() {
        return Result.ok(paramService.listEnabledParams());
    }

    /**
     * 按参数键取值的高频接口。
     * <p>
     * 任何登录用户均可调用（不加 {@code @RequiresPermission}）：
     * 系统参数属于全局只读配置，业务代码需要随时读取税率、开关等运行时配置，
     * 若每次读取都要额外的菜单权限，会让前端在多角色切换时频繁报 403。
     * <p>
     * 参数不存在或已停用时返回空字符串，不抛异常——调用方只需按默认值兜底。
     */
    @GetMapping("/value")
    public Result<String> value(@RequestParam String key) {
        return Result.ok(paramService.getParamValue(key));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:param:query")
    @OperLog(module = "sys")
    public Result<SysParamVO> detail(@PathVariable Long id) {
        return Result.ok(paramService.getParamDetail(id));
    }

    @PostMapping
    @RequiresPermission("sys:param:add")
    @OperLog(module = "sys", type = "CREATE", value = "新增系统参数")
    public Result<Long> create(@Valid @RequestBody SysParamSaveRequest request) {
        return Result.ok(paramService.createParam(request));
    }

    @PutMapping
    @RequiresPermission("sys:param:edit")
    @OperLog(module = "sys", type = "UPDATE", value = "修改系统参数")
    public Result<Void> update(@Valid @RequestBody SysParamSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        paramService.updateParam(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:param:remove")
    @OperLog(module = "sys", type = "DELETE", value = "删除系统参数")
    public Result<Void> delete(@PathVariable Long id) {
        paramService.deleteParam(id);
        return Result.ok();
    }
}













