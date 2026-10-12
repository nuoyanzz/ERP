package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmBomSaveRequest;
import com.erp.pm.domain.vo.PmBomVO;
import com.erp.pm.service.PmBomService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pm/bom")
@RequiredArgsConstructor
public class PmBomController {

    private final PmBomService bomService;

    @GetMapping("/page")
    @RequiresPermission("pm:bom:query")
    @OperLog(module = "pm")
    public Result<PageResult<PmBomVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(bomService.pageBom(pageNum, pageSize, materialId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:bom:query")
    @OperLog(module = "pm")
    public Result<PmBomVO> detail(@PathVariable Long id) {
        return Result.ok(bomService.getBomDetail(id));
    }

    @PostMapping
    @RequiresPermission("pm:bom:add")
    @OperLog(module = "pm")
    public Result<Long> create(@Valid @RequestBody PmBomSaveRequest request) {
        return Result.ok(bomService.createBom(request));
    }

    @PutMapping
    @RequiresPermission("pm:bom:edit")
    @OperLog(module = "pm")
    public Result<Void> update(@Valid @RequestBody PmBomSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        bomService.updateBom(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:bom:remove")
    @OperLog(module = "pm")
    public Result<Void> delete(@PathVariable Long id) {
        bomService.deleteBom(id);
        return Result.ok();
    }
}








