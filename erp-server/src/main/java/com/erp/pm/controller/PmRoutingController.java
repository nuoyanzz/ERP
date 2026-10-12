package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmRoutingOperationRequest;
import com.erp.pm.domain.dto.PmRoutingSaveRequest;
import com.erp.pm.domain.vo.PmRoutingVO;
import com.erp.pm.domain.vo.PmWorkCenterVO;
import com.erp.pm.service.PmRoutingService;
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

import java.util.List;

@RestController
@RequestMapping("/pm/routing")
@RequiredArgsConstructor
public class PmRoutingController {

    private final PmRoutingService routingService;

    @GetMapping("/page")
    @RequiresPermission("pm:routing:query")
    public Result<PageResult<PmRoutingVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) Long materialId,
                                                 @RequestParam(required = false) Integer status) {
        return Result.ok(routingService.pageRouting(pageNum, pageSize, materialId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:routing:query")
    public Result<PmRoutingVO> detail(@PathVariable Long id) {
        return Result.ok(routingService.getRoutingDetail(id));
    }

    @GetMapping("/work-centers")
    @RequiresPermission("pm:routing:query")
    public Result<List<PmWorkCenterVO>> workCenters() {
        return Result.ok(routingService.listWorkCenters());
    }

    @PostMapping
    @RequiresPermission("pm:routing:add")
    @OperLog(module = "pm", type = "CREATE", value = "新增工艺路线")
    public Result<Long> create(@Valid @RequestBody PmRoutingSaveRequest request) {
        return Result.ok(routingService.createRouting(request));
    }

    @PutMapping
    @RequiresPermission("pm:routing:edit")
    @OperLog(module = "pm", type = "UPDATE", value = "修改工艺路线")
    public Result<Void> update(@Valid @RequestBody PmRoutingSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        routingService.updateRouting(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:routing:remove")
    @OperLog(module = "pm", type = "DELETE", value = "删除工艺路线")
    public Result<Void> delete(@PathVariable Long id) {
        routingService.deleteRouting(id);
        return Result.ok();
    }

    @PostMapping("/{id}/operations")
    @RequiresPermission("pm:routing:add")
    @OperLog(module = "pm", type = "CREATE", value = "新增工艺路线工序")
    public Result<Long> addOperation(@PathVariable Long id,
                                     @Valid @RequestBody PmRoutingOperationRequest request) {
        return Result.ok(routingService.addOperation(id, request));
    }

    @PutMapping("/{id}/operations/{operationId}")
    @RequiresPermission("pm:routing:edit")
    @OperLog(module = "pm", type = "UPDATE", value = "修改工艺路线工序")
    public Result<Void> updateOperation(@PathVariable Long id, @PathVariable Long operationId,
                                        @Valid @RequestBody PmRoutingOperationRequest request) {
        routingService.updateOperation(id, operationId, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}/operations/{operationId}")
    @RequiresPermission("pm:routing:remove")
    @OperLog(module = "pm", type = "DELETE", value = "删除工艺路线工序")
    public Result<Void> deleteOperation(@PathVariable Long id, @PathVariable Long operationId) {
        routingService.deleteOperation(id, operationId);
        return Result.ok();
    }
}








