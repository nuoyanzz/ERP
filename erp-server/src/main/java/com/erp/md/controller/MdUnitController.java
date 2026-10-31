package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.MdUnitQueryRequest;
import com.erp.md.domain.dto.MdUnitSaveRequest;
import com.erp.md.domain.vo.MdUnitVO;
import com.erp.md.service.MdUnitService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import jakarta.validation.Valid;
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
@RequestMapping("/md/unit")
@RequiredArgsConstructor
public class MdUnitController {

    private final MdUnitService unitService;

    @GetMapping("/page")
    @RequiresPermission("md:unit:query")
    @OperLog(module = "md")
    public Result<PageResult<MdUnitVO>> page(MdUnitQueryRequest request) {
        return Result.ok(unitService.pageUnit(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:unit:query")
    @OperLog(module = "md")
    public Result<MdUnitVO> detail(@PathVariable Long id) {
        return Result.ok(unitService.getUnitDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:unit:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody MdUnitSaveRequest request) {
        return Result.ok(unitService.createUnit(request));
    }

    @PutMapping
    @RequiresPermission("md:unit:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody MdUnitSaveRequest request) {
        unitService.updateUnit(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:unit:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return Result.ok();
    }
}
