package com.erp.wf.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.wf.domain.dto.WfDefinitionSaveRequest;
import com.erp.wf.domain.vo.WfDefinitionVO;
import com.erp.wf.service.WfDefinitionService;
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

import java.util.List;

@RestController
@RequestMapping("/wf/definition")
@RequiredArgsConstructor
public class WfDefinitionController {

    private final WfDefinitionService definitionService;

    @GetMapping("/page")
    @RequiresPermission("wf:definition:query")
    @OperLog(module = "wf")
    public Result<PageResult<WfDefinitionVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) String definitionKey,
                                                   @RequestParam(required = false) String definitionName,
                                                   @RequestParam(required = false) Integer status) {
        return Result.ok(definitionService.pageDefinition(pageNum, pageSize, definitionKey, definitionName, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("wf:definition:query")
    @OperLog(module = "wf")
    public Result<WfDefinitionVO> detail(@PathVariable Long id) {
        return Result.ok(definitionService.getDefinitionDetail(id));
    }

    /**
     * 按业务类型查询流程定义，供业务模块选择可用流程。
     */
    @GetMapping("/biz-type/{bizType}")
    @RequiresPermission("wf:definition:query")
    public Result<List<WfDefinitionVO>> listByBizType(@PathVariable String bizType,
                                                       @RequestParam(defaultValue = "false") boolean onlyEnabled) {
        return Result.ok(definitionService.listByBizType(bizType, onlyEnabled));
    }

    /**
     * 按业务类型查询已发布的流程定义，未配置时返回 null。
     */
    @GetMapping("/biz-type/{bizType}/published")
    @RequiresPermission("wf:definition:query")
    public Result<WfDefinitionVO> publishedByBizType(@PathVariable String bizType) {
        return Result.ok(definitionService.getPublishedByBizType(bizType));
    }

    @PostMapping
    @RequiresPermission("wf:definition:add")
    @OperLog(module = "wf", type = "CREATE", value = "新增流程定义")
    public Result<Long> create(@Valid @RequestBody WfDefinitionSaveRequest request) {
        return Result.ok(definitionService.createDefinition(request));
    }

    @PutMapping
    @RequiresPermission("wf:definition:edit")
    @OperLog(module = "wf", type = "UPDATE", value = "修改流程定义")
    public Result<Void> update(@Valid @RequestBody WfDefinitionSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        definitionService.updateDefinition(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("wf:definition:remove")
    @OperLog(module = "wf", type = "DELETE", value = "删除流程定义")
    public Result<Void> delete(@PathVariable Long id) {
        definitionService.deleteDefinition(id);
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    @RequiresPermission("wf:definition:publish")
    @OperLog(module = "wf", type = "PUBLISH", value = "发布流程定义")
    public Result<Void> publish(@PathVariable Long id) {
        definitionService.publishDefinition(id);
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    @RequiresPermission("wf:definition:publish")
    @OperLog(module = "wf", type = "DISABLE", value = "停用流程定义")
    public Result<Void> disable(@PathVariable Long id) {
        definitionService.disableDefinition(id);
        return Result.ok();
    }
}








