package com.erp.hr.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrSalaryQueryRequest;
import com.erp.hr.domain.dto.HrSalarySaveRequest;
import com.erp.hr.domain.vo.HrSalaryVO;
import com.erp.hr.service.HrSalaryService;
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
@RequestMapping("/hr/salary")
@RequiredArgsConstructor
public class HrSalaryController {

    private final HrSalaryService salaryService;

    @GetMapping("/page")
    @RequiresPermission("hr:salary:query")
    @OperLog(module = "hr")
    public Result<PageResult<HrSalaryVO>> page(HrSalaryQueryRequest request) {
        return Result.ok(salaryService.pageSalary(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:salary:query")
    @OperLog(module = "hr")
    public Result<HrSalaryVO> detail(@PathVariable Long id) {
        return Result.ok(salaryService.getSalaryDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:salary:add")
    @OperLog(module = "hr")
    public Result<Long> create(@Valid @RequestBody HrSalarySaveRequest request) {
        return Result.ok(salaryService.createSalary(request));
    }

    @PutMapping
    @RequiresPermission("hr:salary:edit")
    @OperLog(module = "hr")
    public Result<Void> update(@Valid @RequestBody HrSalarySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        salaryService.updateSalary(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:salary:remove")
    @OperLog(module = "hr")
    public Result<Void> delete(@PathVariable Long id) {
        salaryService.deleteSalary(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("hr:salary:audit")
    @OperLog(module = "hr")
    public Result<Void> audit(@PathVariable Long id) {
        salaryService.auditSalary(id);
        return Result.ok();
    }
}








