package com.erp.md.controller;

import java.util.List;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.CustomerQueryRequest;
import com.erp.md.domain.dto.CustomerSaveRequest;
import com.erp.md.domain.vo.CustomerVO;
import com.erp.md.service.MdCustomerService;
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
@RequestMapping("/md/customer")
@RequiredArgsConstructor
public class MdCustomerController {

    private final MdCustomerService customerService;

    @GetMapping("/page")
    @RequiresPermission("md:customer:query")
    @OperLog(module = "md")
    public Result<PageResult<CustomerVO>> page(CustomerQueryRequest request) {
        return Result.ok(customerService.pageCustomer(request));
    }

    @GetMapping("/list")
    @RequiresPermission("md:customer:query")
    @OperLog(module = "md")
    public Result<List<CustomerVO>> list() {
        return Result.ok(customerService.listMdCustomer());
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:customer:query")
    @OperLog(module = "md")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.ok(customerService.getCustomerDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:customer:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody CustomerSaveRequest request) {
        return Result.ok(customerService.createCustomer(request));
    }

    @PutMapping
    @RequiresPermission("md:customer:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody CustomerSaveRequest request) {
        customerService.updateCustomer(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:customer:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.ok();
    }
}








