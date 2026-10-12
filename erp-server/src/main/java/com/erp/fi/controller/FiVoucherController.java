package com.erp.fi.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherQueryRequest;
import com.erp.fi.domain.dto.VoucherSaveRequest;
import com.erp.fi.domain.vo.VoucherDetailVO;
import com.erp.fi.domain.vo.VoucherVO;
import com.erp.fi.service.FiVoucherService;
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

@RestController
@RequestMapping("/fi/voucher")
@RequiredArgsConstructor
public class FiVoucherController {

    private final FiVoucherService voucherService;

    @GetMapping("/page")
    @RequiresPermission("fi:voucher:query")
    public Result<PageResult<VoucherVO>> page(VoucherQueryRequest request) {
        return Result.ok(voucherService.pageVoucher(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("fi:voucher:query")
    public Result<VoucherDetailVO> detail(@PathVariable Long id) {
        return Result.ok(voucherService.getVoucherDetail(id));
    }

    @PostMapping
    @RequiresPermission("fi:voucher:add")
    @OperLog(module = "fi", type = "CREATE", value = "新增记账凭证")
    public Result<Long> create(@Valid @RequestBody VoucherSaveRequest request) {
        return Result.ok(voucherService.createVoucher(request));
    }

    @PutMapping
    @RequiresPermission("fi:voucher:edit")
    @OperLog(module = "fi", type = "UPDATE", value = "修改记账凭证")
    public Result<Void> update(@Valid @RequestBody VoucherSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        voucherService.updateVoucher(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("fi:voucher:remove")
    @OperLog(module = "fi", type = "DELETE", value = "删除记账凭证")
    public Result<Void> delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission("fi:voucher:submit")
    @OperLog(module = "fi", type = "SUBMIT", value = "提交记账凭证")
    public Result<Void> submit(@PathVariable Long id) {
        voucherService.submitVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("fi:voucher:audit")
    @OperLog(module = "fi", type = "AUDIT", value = "审核记账凭证")
    public Result<Void> audit(@PathVariable Long id) {
        voucherService.auditVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/unaudit")
    @RequiresPermission("fi:voucher:unaudit")
    @OperLog(module = "fi", type = "UNAUDIT", value = "反审核记账凭证")
    public Result<Void> unaudit(@PathVariable Long id) {
        voucherService.unauditVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/post")
    @RequiresPermission("fi:voucher:post")
    @OperLog(module = "fi", type = "POST", value = "记账")
    public Result<Void> post(@PathVariable Long id) {
        voucherService.postVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/unpost")
    @RequiresPermission("fi:voucher:unpost")
    @OperLog(module = "fi", type = "UNPOST", value = "反记账")
    public Result<Void> unpost(@PathVariable Long id) {
        voucherService.unpostVoucher(id);
        return Result.ok();
    }

    @PostMapping("/{id}/void")
    @RequiresPermission("fi:voucher:void")
    @OperLog(module = "fi", type = "VOID", value = "作废记账凭证")
    public Result<Void> voidVoucher(@PathVariable Long id, @RequestParam(required = false) String reason) {
        voucherService.voidVoucher(id, reason);
        return Result.ok();
    }

    @PostMapping("/{id}/red-flush")
    @RequiresPermission("fi:voucher:redFlush")
    @OperLog(module = "fi", type = "RED_FLUSH", value = "红字冲销凭证")
    public Result<Long> redFlush(@PathVariable Long id) {
        return Result.ok(voucherService.redFlushVoucher(id));
    }
}








