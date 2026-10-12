package com.erp.pa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pa.domain.dto.PaOrderCloseRequest;
import com.erp.pa.domain.dto.PaOrderQueryRequest;
import com.erp.pa.domain.dto.PaOrderSaveRequest;
import com.erp.pa.domain.vo.PaOrderDetailVO;
import com.erp.pa.domain.vo.PaOrderListVO;
import com.erp.pa.service.PaOrderService;
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
@RequestMapping("/pa/order")
@RequiredArgsConstructor
public class PaOrderController {

    private final PaOrderService orderService;

    @GetMapping("/page")
    @RequiresPermission("pa:order:query")
    @OperLog(module = "pa")
    public Result<PageResult<PaOrderListVO>> page(PaOrderQueryRequest request) {
        return Result.ok(orderService.pageOrder(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pa:order:query")
    @OperLog(module = "pa")
    public Result<PaOrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id));
    }

    @PostMapping
    @RequiresPermission("pa:order:add")
    @OperLog(module = "pa")
    public Result<Long> create(@Valid @RequestBody PaOrderSaveRequest request) {
        return Result.ok(orderService.createOrder(request));
    }

    @PutMapping
    @RequiresPermission("pa:order:edit")
    @OperLog(module = "pa")
    public Result<Void> update(@Valid @RequestBody PaOrderSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        orderService.updateOrder(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pa:order:remove")
    @OperLog(module = "pa")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pa:order:audit")
    @OperLog(module = "pa")
    public Result<Void> audit(@PathVariable Long id) {
        orderService.auditOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    @RequiresPermission("pa:order:close")
    @OperLog(module = "pa")
    public Result<Void> close(@PathVariable Long id, @RequestBody(required = false) PaOrderCloseRequest request) {
        orderService.closeOrder(id, request);
        return Result.ok();
    }
}








