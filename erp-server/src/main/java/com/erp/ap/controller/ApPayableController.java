package com.erp.ap.controller;

import com.erp.ap.domain.dto.ApPayableQueryRequest;
import com.erp.ap.domain.vo.ApPayableVO;
import com.erp.ap.service.ApPayableService;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ap/payable")
@RequiredArgsConstructor
public class ApPayableController {

    private final ApPayableService payableService;

    @GetMapping("/page")
    @RequiresPermission("ap:payable:query")
    @OperLog(module = "ap")
    public Result<PageResult<ApPayableVO>> page(ApPayableQueryRequest request) {
        return Result.ok(payableService.pagePayable(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ap:payable:query")
    @OperLog(module = "ap")
    public Result<ApPayableVO> detail(@PathVariable Long id) {
        return Result.ok(payableService.getPayableDetail(id));
    }
}








