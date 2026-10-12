package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.MaterialQueryRequest;
import com.erp.md.domain.dto.MaterialSaveRequest;
import com.erp.md.domain.vo.MaterialVO;
import com.erp.md.service.MdMaterialService;
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
@RequestMapping("/md/material")
@RequiredArgsConstructor
public class MdMaterialController {

    private final MdMaterialService materialService;

    @GetMapping("/page")
    @RequiresPermission("md:material:query")
    @OperLog(module = "md")
    public Result<PageResult<MaterialVO>> page(MaterialQueryRequest request) {
        return Result.ok(materialService.pageMaterial(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:material:query")
    @OperLog(module = "md")
    public Result<MaterialVO> detail(@PathVariable Long id) {
        return Result.ok(materialService.getMaterialDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:material:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody MaterialSaveRequest request) {
        return Result.ok(materialService.createMaterial(request));
    }

    @PutMapping
    @RequiresPermission("md:material:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody MaterialSaveRequest request) {
        materialService.updateMaterial(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:material:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return Result.ok();
    }
}








