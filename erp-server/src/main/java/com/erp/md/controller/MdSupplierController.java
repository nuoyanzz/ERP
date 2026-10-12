package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.SupplierQueryRequest;
import com.erp.md.domain.dto.SupplierSaveRequest;
import com.erp.md.domain.vo.SupplierVO;
import com.erp.md.service.MdSupplierService;
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
@RequestMapping("/md/supplier")
@RequiredArgsConstructor
public class MdSupplierController {

    private final MdSupplierService supplierService;

    @GetMapping("/page")
    @RequiresPermission("md:supplier:query")
    @OperLog(module = "md")
    public Result<PageResult<SupplierVO>> page(SupplierQueryRequest request) {
        return Result.ok(supplierService.pageSupplier(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:supplier:query")
    @OperLog(module = "md")
    public Result<SupplierVO> detail(@PathVariable Long id) {
        return Result.ok(supplierService.getSupplierDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:supplier:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody SupplierSaveRequest request) {
        return Result.ok(supplierService.createSupplier(request));
    }

    @PutMapping
    @RequiresPermission("md:supplier:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody SupplierSaveRequest request) {
        supplierService.updateSupplier(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:supplier:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.ok();
    }
}








