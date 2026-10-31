package com.erp.wf.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import com.erp.wf.domain.dto.WfApproveRequest;
import com.erp.wf.domain.dto.WfCountersignRequest;
import com.erp.wf.domain.dto.WfTransferRequest;
import com.erp.wf.domain.vo.WfTaskVO;
import com.erp.wf.service.WfTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wf/task")
@RequiredArgsConstructor
public class WfTaskController {

    private final WfTaskService taskService;

    @GetMapping("/todo")
    @RequiresPermission("wf:task:todo")
    @OperLog(module = "wf")
    public Result<PageResult<WfTaskVO>> todo(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String bizType,
                                             @RequestParam(required = false) Long bizId) {
        return Result.ok(taskService.todoPage(pageNum, pageSize, bizType, bizId));
    }

    @GetMapping("/done")
    @RequiresPermission("wf:task:done")
    @OperLog(module = "wf")
    public Result<PageResult<WfTaskVO>> done(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String bizType,
                                             @RequestParam(required = false) Long bizId) {
        return Result.ok(taskService.donePage(pageNum, pageSize, bizType, bizId));
    }

    /**
     * 查询某个流程实例下的全部任务，用于审批记录展示。
     */
    @GetMapping("/instance/{instanceId}")
    @RequiresPermission("wf:instance:query")
    public Result<PageResult<WfTaskVO>> listByInstance(@PathVariable Long instanceId,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "50") int pageSize) {
        return Result.ok(taskService.pageInstanceTasks(instanceId, pageNum, pageSize));
    }

    @PostMapping("/{id}/approve")
    @RequiresPermission("wf:task:approve")
    @OperLog(module = "wf", type = "APPROVE", value = "审批同意")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody(required = false) WfApproveRequest request) {
        taskService.approveTask(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @RequiresPermission("wf:task:reject")
    @OperLog(module = "wf", type = "REJECT", value = "审批驳回")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) WfApproveRequest request) {
        taskService.rejectTask(id, request);
        return Result.ok();
    }

    /**
     * 退回到上一节点重新审批。
     */
    @PostMapping("/{id}/rollback")
    @RequiresPermission("wf:task:rollback")
    @OperLog(module = "wf", type = "ROLLBACK", value = "审批退回")
    public Result<Void> rollback(@PathVariable Long id, @Valid @RequestBody(required = false) WfApproveRequest request) {
        taskService.rollbackTask(id, request);
        return Result.ok();
    }

    /**
     * 转办：把待办转给其他人。
     */
    @PostMapping("/{id}/transfer")
    @RequiresPermission("wf:task:transfer")
    @OperLog(module = "wf", type = "TRANSFER", value = "任务转办")
    public Result<Void> transfer(@PathVariable Long id, @Valid @RequestBody WfTransferRequest request) {
        taskService.transferTask(id, request.getTargetUserId(), request.getComment());
        return Result.ok();
    }

    /**
     * 加签：在当前节点增加审批人。
     */
    @PostMapping("/{id}/countersign")
    @RequiresPermission("wf:task:countersign")
    @OperLog(module = "wf", type = "COUNTERSIGN", value = "任务加签")
    public Result<Void> countersign(@PathVariable Long id, @Valid @RequestBody WfCountersignRequest request) {
        taskService.countersignTask(id, request.getTargetUserId(), request.getPosition(), request.getComment());
        return Result.ok();
    }

    /**
     * 催办：提醒待办审批人尽快处理。
     */
    @PostMapping("/{id}/urge")
    @RequiresPermission("wf:task:approve")
    @OperLog(module = "wf", type = "URGE", value = "任务催办")
    public Result<Void> urge(@PathVariable Long id) {
        taskService.urgeTask(id);
        return Result.ok();
    }
}








