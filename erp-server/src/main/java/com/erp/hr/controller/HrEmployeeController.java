package com.erp.hr.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrEmployeeQueryRequest;
import com.erp.hr.domain.dto.HrEmployeeSaveRequest;
import com.erp.hr.domain.vo.HrEmployeeVO;
import com.erp.hr.service.HrEmployeeService;
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
@RequestMapping("/hr/employee")
@RequiredArgsConstructor
public class HrEmployeeController {

    private final HrEmployeeService employeeService;

    @GetMapping("/page")
    @RequiresPermission("hr:employee:query")
    @OperLog(module = "hr")
    public Result<PageResult<HrEmployeeVO>> page(HrEmployeeQueryRequest request) {
        return Result.ok(employeeService.pageEmployee(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:employee:query")
    @OperLog(module = "hr")
    public Result<HrEmployeeVO> detail(@PathVariable Long id) {
        return Result.ok(employeeService.getEmployeeDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:employee:add")
    @OperLog(module = "hr")
    public Result<Long> create(@Valid @RequestBody HrEmployeeSaveRequest request) {
        return Result.ok(employeeService.createEmployee(request));
    }

    @PutMapping
    @RequiresPermission("hr:employee:edit")
    @OperLog(module = "hr")
    public Result<Void> update(@Valid @RequestBody HrEmployeeSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        employeeService.updateEmployee(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:employee:remove")
    @OperLog(module = "hr")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.ok();
    }
}








