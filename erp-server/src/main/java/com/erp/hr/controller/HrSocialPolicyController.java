package com.erp.hr.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.SocialPolicySaveRequest;
import com.erp.hr.domain.vo.SocialPolicyVO;
import com.erp.hr.service.HrSocialPolicyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社保个税政策维护。
 */
@RestController
@RequestMapping("/hr/policy")
@RequiredArgsConstructor
public class HrSocialPolicyController {

    private final HrSocialPolicyService policyService;

    @GetMapping("/list")
    @RequiresPermission("hr:policy:query")
    @OperLog(module = "hr")
    public Result<List<SocialPolicyVO>> list() {
        return Result.ok(policyService.listPolicies());
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:policy:query")
    @OperLog(module = "hr")
    public Result<SocialPolicyVO> detail(@PathVariable Long id) {
        return Result.ok(policyService.getPolicyDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:policy:add")
    @OperLog(module = "hr", type = "CREATE", value = "新增社保个税政策")
    public Result<Long> create(@Valid @RequestBody SocialPolicySaveRequest request) {
        request.setId(null);
        return Result.ok(policyService.savePolicy(request));
    }

    @PutMapping
    @RequiresPermission("hr:policy:edit")
    @OperLog(module = "hr", type = "UPDATE", value = "修改社保个税政策")
    public Result<Void> update(@Valid @RequestBody SocialPolicySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        policyService.savePolicy(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:policy:remove")
    @OperLog(module = "hr", type = "DELETE", value = "删除社保个税政策")
    public Result<Void> remove(@PathVariable Long id) {
        policyService.removePolicy(id);
        return Result.ok();
    }
}
