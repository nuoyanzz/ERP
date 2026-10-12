package com.erp.ap.controller;

import com.erp.ap.domain.dto.ApPaymentSaveRequest;
import com.erp.ap.domain.vo.ApPaymentVO;
import com.erp.ap.service.ApPaymentService;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
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
@RequestMapping("/ap/payment")
@RequiredArgsConstructor
public class ApPaymentController {

    private final ApPaymentService paymentService;

    @GetMapping("/page")
    @RequiresPermission("ap:payment:query")
    @OperLog(module = "ap")
    public Result<PageResult<ApPaymentVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Long supplierId,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(paymentService.pagePayment(pageNum, pageSize, supplierId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ap:payment:query")
    @OperLog(module = "ap")
    public Result<ApPaymentVO> detail(@PathVariable Long id) {
        return Result.ok(paymentService.getPaymentDetail(id));
    }

    @PostMapping
    @RequiresPermission("ap:payment:add")
    @OperLog(module = "ap")
    public Result<Long> create(@Valid @RequestBody ApPaymentSaveRequest request) {
        return Result.ok(paymentService.createPayment(request));
    }

    @PutMapping
    @RequiresPermission("ap:payment:edit")
    @OperLog(module = "ap")
    public Result<Void> update(@Valid @RequestBody ApPaymentSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        paymentService.updatePayment(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ap:payment:remove")
    @OperLog(module = "ap")
    public Result<Void> delete(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("ap:payment:audit")
    @OperLog(module = "ap")
    public Result<Void> audit(@PathVariable Long id) {
        paymentService.auditPayment(id);
        return Result.ok();
    }
}








