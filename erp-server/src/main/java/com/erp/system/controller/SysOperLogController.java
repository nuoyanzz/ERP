package com.erp.system.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import com.erp.system.domain.dto.OperLogQueryRequest;
import com.erp.system.domain.entity.SysOperLog;
import com.erp.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询。
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService operLogService;

    @GetMapping("/page")
    @RequiresPermission("sys:log:query")
    @OperLog(module = "system")
    public Result<PageResult<SysOperLog>> page(OperLogQueryRequest request) {
        return Result.ok(operLogService.pageLogs(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:log:query")
    @OperLog(module = "system")
    public Result<SysOperLog> detail(@PathVariable Long id) {
        return Result.ok(operLogService.getLogDetail(id));
    }
}
