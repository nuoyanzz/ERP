package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.domain.vo.TrialBalanceVO;
import com.erp.fi.service.FiOpeningBalanceService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试算平衡。生成指定期间的试算平衡表，作为结账前的强制校验项。
 */
@RestController
@RequestMapping("/fi/trial")
@RequiredArgsConstructor
public class FiTrialBalanceController {

    private final FiOpeningBalanceService openingBalanceService;

    @GetMapping("/balance")
    @RequiresPermission("fi:trial:query")
    @OperLog(module = "fi", type = "TRIAL_BALANCE", value = "生成试算平衡表")
    public Result<TrialBalanceVO> trialBalance(@RequestParam String period) {
        return Result.ok(openingBalanceService.generateTrialBalance(period));
    }
}
