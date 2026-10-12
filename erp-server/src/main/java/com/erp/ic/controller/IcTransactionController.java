package com.erp.ic.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.ic.domain.dto.TransactionQueryRequest;
import com.erp.ic.domain.vo.IcTransactionVO;
import com.erp.ic.service.IcStockService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ic/transaction")
@RequiredArgsConstructor
public class IcTransactionController {

    private final IcStockService stockService;

    @GetMapping("/page")
    @RequiresPermission("ic:transaction:query")
    @OperLog(module = "ic")
    public Result<PageResult<IcTransactionVO>> page(TransactionQueryRequest request) {
        return Result.ok(stockService.pageTransaction(request));
    }
}








