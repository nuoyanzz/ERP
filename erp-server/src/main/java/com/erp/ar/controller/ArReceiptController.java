package com.erp.ar.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.ar.domain.dto.ArReceivableQueryRequest;
import com.erp.ar.domain.dto.ArReceiptSaveRequest;
import com.erp.ar.domain.vo.ArAgingVO;
import com.erp.ar.domain.vo.ArReceiptVO;
import com.erp.ar.domain.vo.ArReceivableVO;
import com.erp.ar.service.ArReceiptService;
import com.erp.ar.service.ArReceivableService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ar/receipt")
@RequiredArgsConstructor
public class ArReceiptController {

    private final ArReceiptService receiptService;

    @GetMapping("/page")
    @RequiresPermission("ar:receipt:query")
    @OperLog(module = "ar")
    public Result<PageResult<ArReceiptVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Long customerId,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(receiptService.pageReceipt(pageNum, pageSize, customerId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ar:receipt:query")
    @OperLog(module = "ar")
    public Result<ArReceiptVO> detail(@PathVariable Long id) {
        return Result.ok(receiptService.getReceiptDetail(id));
    }

    @PostMapping
    @RequiresPermission("ar:receipt:add")
    @OperLog(module = "ar")
    public Result<Long> create(@Valid @RequestBody ArReceiptSaveRequest request) {
        return Result.ok(receiptService.createReceipt(request));
    }

    @PutMapping
    @RequiresPermission("ar:receipt:edit")
    @OperLog(module = "ar")
    public Result<Void> update(@Valid @RequestBody ArReceiptSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        receiptService.updateReceipt(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ar:receipt:remove")
    @OperLog(module = "ar")
    public Result<Void> delete(@PathVariable Long id) {
        receiptService.deleteReceipt(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("ar:receipt:audit")
    @OperLog(module = "ar")
    public Result<Void> audit(@PathVariable Long id) {
        receiptService.auditReceipt(id);
        return Result.ok();
    }
}








