package com.erp.ar.controller;

import com.erp.ar.domain.dto.ArReceivableQueryRequest;
import com.erp.ar.domain.vo.ArAgingVO;
import com.erp.ar.domain.vo.ArReceivableVO;
import com.erp.ar.service.ArReceivableService;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ar/receivable")
@RequiredArgsConstructor
public class ArReceivableController {

    private final ArReceivableService receivableService;

    @GetMapping("/page")
    @RequiresPermission("ar:receivable:query")
    @OperLog(module = "ar")
    public Result<PageResult<ArReceivableVO>> page(ArReceivableQueryRequest request) {
        return Result.ok(receivableService.pageReceivable(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ar:receivable:query")
    @OperLog(module = "ar")
    public Result<ArReceivableVO> detail(@PathVariable Long id) {
        return Result.ok(receivableService.getReceivableDetail(id));
    }

    @GetMapping("/aging")
    @RequiresPermission("ar:receivable:query")
    @OperLog(module = "ar")
    public Result<List<ArAgingVO>> aging(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return Result.ok(receivableService.aging(asOfDate));
    }
}








