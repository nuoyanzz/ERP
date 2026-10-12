package com.erp.hr.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrDeptSaveRequest;
import com.erp.hr.domain.vo.HrDeptTreeVO;
import com.erp.hr.service.HrDeptService;
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

import java.util.List;

@RestController
@RequestMapping("/hr/dept")
@RequiredArgsConstructor
public class HrDeptController {

    private final HrDeptService deptService;

    @GetMapping("/tree")
    @RequiresPermission("hr:dept:query")
    @OperLog(module = "hr")
    public Result<List<HrDeptTreeVO>> tree() {
        return Result.ok(deptService.treeDepts());
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:dept:query")
    @OperLog(module = "hr")
    public Result<HrDeptTreeVO> detail(@PathVariable Long id) {
        return Result.ok(deptService.getDeptDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:dept:add")
    @OperLog(module = "hr")
    public Result<Long> create(@Valid @RequestBody HrDeptSaveRequest request) {
        return Result.ok(deptService.createDept(request));
    }

    @PutMapping
    @RequiresPermission("hr:dept:edit")
    @OperLog(module = "hr")
    public Result<Void> update(@Valid @RequestBody HrDeptSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        deptService.updateDept(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:dept:remove")
    @OperLog(module = "hr")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.ok();
    }
}








