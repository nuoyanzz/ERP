package com.erp.hr.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrOrgSaveRequest;
import com.erp.hr.domain.vo.HrOrgVO;
import com.erp.hr.service.HrOrgService;
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
@RequestMapping("/hr/org")
@RequiredArgsConstructor
public class HrOrgController {

    private final HrOrgService orgService;

    @GetMapping("/page")
    @RequiresPermission("hr:org:query")
    @OperLog(module = "hr")
    public Result<com.erp.common.api.PageResult<HrOrgVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                                @RequestParam(defaultValue = "10") int pageSize,
                                                                @RequestParam(required = false) String orgName) {
        return Result.ok(orgService.pageOrg(pageNum, pageSize, orgName));
    }

    @GetMapping("/list")
    @RequiresPermission("hr:org:query")
    @OperLog(module = "hr")
    public Result<List<HrOrgVO>> list() {
        return Result.ok(orgService.listOrgs());
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:org:query")
    @OperLog(module = "hr")
    public Result<HrOrgVO> detail(@PathVariable Long id) {
        return Result.ok(orgService.getOrgDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:org:add")
    @OperLog(module = "hr")
    public Result<Long> create(@Valid @RequestBody HrOrgSaveRequest request) {
        return Result.ok(orgService.createOrg(request));
    }

    @PutMapping
    @RequiresPermission("hr:org:edit")
    @OperLog(module = "hr")
    public Result<Void> update(@Valid @RequestBody HrOrgSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        orgService.updateOrg(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:org:remove")
    @OperLog(module = "hr")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.deleteOrg(id);
        return Result.ok();
    }
}








