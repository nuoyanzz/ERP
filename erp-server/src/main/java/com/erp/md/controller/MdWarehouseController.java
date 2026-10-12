package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.WarehouseQueryRequest;
import com.erp.md.domain.dto.WarehouseSaveRequest;
import com.erp.md.domain.vo.WarehouseVO;
import com.erp.md.service.MdWarehouseService;
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

@RestController
@RequestMapping("/md/warehouse")
@RequiredArgsConstructor
public class MdWarehouseController {

    private final MdWarehouseService warehouseService;

    @GetMapping("/page")
    @RequiresPermission("md:warehouse:query")
    @OperLog(module = "md")
    public Result<PageResult<WarehouseVO>> page(WarehouseQueryRequest request) {
        return Result.ok(warehouseService.pageWarehouse(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:warehouse:query")
    @OperLog(module = "md")
    public Result<WarehouseVO> detail(@PathVariable Long id) {
        return Result.ok(warehouseService.getWarehouseDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:warehouse:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody WarehouseSaveRequest request) {
        return Result.ok(warehouseService.createWarehouse(request));
    }

    @PutMapping
    @RequiresPermission("md:warehouse:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody WarehouseSaveRequest request) {
        warehouseService.updateWarehouse(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:warehouse:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.ok();
    }
}








