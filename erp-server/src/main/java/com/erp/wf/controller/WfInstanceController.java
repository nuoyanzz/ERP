package com.erp.wf.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.wf.domain.dto.WfInstanceQueryRequest;
import com.erp.wf.domain.dto.WfStartRequest;
import com.erp.wf.domain.dto.WfWithdrawRequest;
import com.erp.wf.domain.vo.WfInstanceDetailVO;
import com.erp.wf.domain.vo.WfInstanceVO;
import com.erp.wf.service.WfInstanceService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wf/instance")
@RequiredArgsConstructor
public class WfInstanceController {

    private final WfInstanceService instanceService;

    @GetMapping("/page")
    @RequiresPermission("wf:instance:query")
    @OperLog(module = "wf")
    public Result<PageResult<WfInstanceVO>> page(WfInstanceQueryRequest request) {
        return Result.ok(instanceService.pageInstance(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("wf:instance:query")
    @OperLog(module = "wf")
    public Result<WfInstanceDetailVO> detail(@PathVariable Long id) {
        return Result.ok(instanceService.getInstanceDetail(id));
    }

    @PostMapping("/start")
    @RequiresPermission("wf:instance:add")
    @OperLog(module = "wf")
    public Result<Long> start(@Valid @RequestBody WfStartRequest request) {
        return Result.ok(instanceService.startInstance(request));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("wf:instance:add")
    @OperLog(module = "wf", type = "CANCEL", value = "撤销流程实例")
    public Result<Void> cancel(@PathVariable Long id) {
        instanceService.cancelInstance(id);
        return Result.ok();
    }

    /**
     * 撤回：发起人在无人处理时撤回自己发起的流程。
     */
    @PostMapping("/{id}/withdraw")
    @RequiresPermission("wf:instance:add")
    @OperLog(module = "wf", type = "WITHDRAW", value = "撤回流程实例")
    public Result<Void> withdraw(@PathVariable Long id,
                                 @RequestBody(required = false) WfWithdrawRequest request) {
        instanceService.withdrawInstance(id, request == null ? null : request.getComment());
        return Result.ok();
    }
}








