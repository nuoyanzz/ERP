package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.service.FiCarryForwardService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fi/carry-forward")
@RequiredArgsConstructor
public class FiCarryForwardController {

    private final FiCarryForwardService carryForwardService;

    @PostMapping("/profit-and-loss")
    @RequiresPermission("fi:carry:execute")
    @OperLog(module = "fi")
    public Result<Long> carryForward(@RequestParam String period) {
        return Result.ok(carryForwardService.carryForwardProfitAndLoss(period));
    }
}








