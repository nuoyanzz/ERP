package com.erp.ic.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.ic.domain.dto.IcTransferSaveRequest;
import com.erp.ic.domain.vo.IcTransferDetailVO;
import com.erp.ic.domain.vo.IcTransferListVO;
import com.erp.ic.service.IcTransferService;
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
@RequestMapping("/ic/transfer")
@RequiredArgsConstructor
public class IcTransferController {

    private final IcTransferService transferService;

    @GetMapping("/page")
    @RequiresPermission("ic:transfer:query")
    @OperLog(module = "ic")
    public Result<PageResult<IcTransferListVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) Long fromWarehouseId,
                                                     @RequestParam(required = false) Long toWarehouseId,
                                                     @RequestParam(required = false) Integer status) {
        return Result.ok(transferService.pageTransfer(pageNum, pageSize, fromWarehouseId, toWarehouseId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ic:transfer:query")
    @OperLog(module = "ic")
    public Result<IcTransferDetailVO> detail(@PathVariable Long id) {
        return Result.ok(transferService.getTransferDetail(id));
    }

    @PostMapping
    @RequiresPermission("ic:transfer:add")
    @OperLog(module = "ic")
    public Result<Long> create(@Valid @RequestBody IcTransferSaveRequest request) {
        return Result.ok(transferService.createTransfer(request));
    }

    @PutMapping
    @RequiresPermission("ic:transfer:edit")
    @OperLog(module = "ic")
    public Result<Void> update(@Valid @RequestBody IcTransferSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        transferService.updateTransfer(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ic:transfer:remove")
    @OperLog(module = "ic")
    public Result<Void> delete(@PathVariable Long id) {
        transferService.deleteTransfer(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("ic:transfer:audit")
    @OperLog(module = "ic")
    public Result<Void> audit(@PathVariable Long id) {
        transferService.auditTransfer(id);
        return Result.ok();
    }
}








