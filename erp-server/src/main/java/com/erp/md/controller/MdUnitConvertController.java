package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.MdUnitConvertQueryRequest;
import com.erp.md.domain.dto.MdUnitConvertSaveRequest;
import com.erp.md.domain.vo.MdUnitConvertVO;
import com.erp.md.service.MdUnitConvertService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/md/unit-convert")
@RequiredArgsConstructor
public class MdUnitConvertController {

    private final MdUnitConvertService unitConvertService;

    @GetMapping("/page")
    @RequiresPermission("md:unitConvert:query")
    @OperLog(module = "md")
    public Result<PageResult<MdUnitConvertVO>> page(MdUnitConvertQueryRequest request) {
        return Result.ok(unitConvertService.pageConvert(request));
    }

    /**
     * 按物料与来源单位查询换算率，供业务单据做单位换算使用。
     * <p>
     * 未配置换算关系时返回 1，即按 1:1 换算。
     */
    @GetMapping("/rate")
    @RequiresPermission("md:unitConvert:query")
    @OperLog(module = "md")
    public Result<BigDecimal> rate(@RequestParam Long materialId, @RequestParam String unit) {
        return Result.ok(unitConvertService.getConvertRate(materialId, unit));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:unitConvert:query")
    @OperLog(module = "md")
    public Result<MdUnitConvertVO> detail(@PathVariable Long id) {
        return Result.ok(unitConvertService.getConvertDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:unitConvert:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody MdUnitConvertSaveRequest request) {
        return Result.ok(unitConvertService.createConvert(request));
    }

    @PutMapping
    @RequiresPermission("md:unitConvert:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody MdUnitConvertSaveRequest request) {
        unitConvertService.updateConvert(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:unitConvert:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        unitConvertService.deleteConvert(id);
        return Result.ok();
    }
}
