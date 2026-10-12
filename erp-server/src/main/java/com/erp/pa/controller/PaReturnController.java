package com.erp.pa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pa.domain.dto.PaReturnQueryRequest;
import com.erp.pa.domain.dto.PaReturnSaveRequest;
import com.erp.pa.domain.vo.PaReturnDetailVO;
import com.erp.pa.domain.vo.PaReturnListVO;
import com.erp.pa.service.PaReturnService;
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
@RequestMapping("/pa/return")
@RequiredArgsConstructor
public class PaReturnController {

    private final PaReturnService returnService;

    @GetMapping("/page")
    @RequiresPermission("pa:return:query")
    @OperLog(module = "pa")
    public Result<PageResult<PaReturnListVO>> page(PaReturnQueryRequest request) {
        return Result.ok(returnService.pageReturn(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pa:return:query")
    @OperLog(module = "pa")
    public Result<PaReturnDetailVO> detail(@PathVariable Long id) {
        return Result.ok(returnService.getReturnDetail(id));
    }

    @PostMapping
    @RequiresPermission("pa:return:add")
    @OperLog(module = "pa")
    public Result<Long> create(@Valid @RequestBody PaReturnSaveRequest request) {
        return Result.ok(returnService.createReturn(request));
    }

    @PutMapping
    @RequiresPermission("pa:return:edit")
    @OperLog(module = "pa")
    public Result<Void> update(@Valid @RequestBody PaReturnSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        returnService.updateReturn(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pa:return:remove")
    @OperLog(module = "pa")
    public Result<Void> delete(@PathVariable Long id) {
        returnService.deleteReturn(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pa:return:audit")
    @OperLog(module = "pa")
    public Result<Void> audit(@PathVariable Long id) {
        returnService.auditReturn(id);
        return Result.ok();
    }
}








