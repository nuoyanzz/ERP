package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.domain.vo.FiAccountBalanceVO;
import com.erp.fi.service.FiBalanceService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fi/balance")
@RequiredArgsConstructor
public class FiBalanceController {

    private final FiBalanceService balanceService;

    @GetMapping("/account")
    @RequiresPermission("fi:balance:query")
    @OperLog(module = "fi")
    public Result<List<FiAccountBalanceVO>> accountBalance(@RequestParam String period) {
        return Result.ok(balanceService.getAccountBalance(period));
    }
}








