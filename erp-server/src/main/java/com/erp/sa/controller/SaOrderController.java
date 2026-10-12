package com.erp.sa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.sa.domain.dto.SaOrderCloseRequest;
import com.erp.sa.domain.dto.SaOrderQueryRequest;
import com.erp.sa.domain.dto.SaOrderSaveRequest;
import com.erp.sa.domain.vo.SaOrderDetailVO;
import com.erp.sa.domain.vo.SaOrderListVO;
import com.erp.sa.service.SaOrderService;
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
@RequestMapping("/sa/order")
@RequiredArgsConstructor
public class SaOrderController {

    private final SaOrderService orderService;

    @GetMapping("/page")
    @RequiresPermission("sa:order:query")
    @OperLog(module = "sa")
    public Result<PageResult<SaOrderListVO>> page(SaOrderQueryRequest request) {
        return Result.ok(orderService.pageOrder(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sa:order:query")
    @OperLog(module = "sa")
    public Result<SaOrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id));
    }

    @PostMapping
    @RequiresPermission("sa:order:add")
    @OperLog(module = "sa")
    public Result<Long> create(@Valid @RequestBody SaOrderSaveRequest request) {
        return Result.ok(orderService.createOrder(request));
    }

    @PutMapping
    @RequiresPermission("sa:order:edit")
    @OperLog(module = "sa")
    public Result<Void> update(@Valid @RequestBody SaOrderSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        orderService.updateOrder(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sa:order:remove")
    @OperLog(module = "sa")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("sa:order:audit")
    @OperLog(module = "sa")
    public Result<Void> audit(@PathVariable Long id) {
        orderService.auditOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    @RequiresPermission("sa:order:close")
    @OperLog(module = "sa")
    public Result<Void> close(@PathVariable Long id, @RequestBody(required = false) SaOrderCloseRequest request) {
        orderService.closeOrder(id, request);
        return Result.ok();
    }
}








