package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.domain.dto.OpeningBalanceRequest;
import com.erp.fi.domain.vo.OpeningBalanceVO;
import com.erp.fi.domain.vo.TrialBalanceVO;
import com.erp.fi.service.FiOpeningBalanceService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fi/opening")
@RequiredArgsConstructor
public class FiOpeningBalanceController {

    private final FiOpeningBalanceService openingBalanceService;

    @GetMapping("/list")
    @RequiresPermission("fi:opening:query")
    @OperLog(module = "fi")
    public Result<List<OpeningBalanceVO>> list(@RequestParam String period) {
        return Result.ok(openingBalanceService.listOpeningBalances(period));
    }

    @PostMapping
    @RequiresPermission("fi:opening:add")
    @OperLog(module = "fi", type = "CREATE", value = "录入期初余额")
    public Result<Long> save(@Valid @RequestBody OpeningBalanceRequest request) {
        return Result.ok(openingBalanceService.saveOpeningBalance(request));
    }

    @PostMapping("/import")
    @RequiresPermission("fi:opening:import")
    @OperLog(module = "fi", type = "IMPORT", value = "批量导入期初余额")
    public Result<Integer> importBatch(@RequestBody List<OpeningBalanceRequest> requests) {
        return Result.ok(openingBalanceService.importOpeningBalances(requests));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("fi:opening:remove")
    @OperLog(module = "fi", type = "DELETE", value = "删除期初余额")
    public Result<Void> delete(@PathVariable Long id) {
        openingBalanceService.deleteOpeningBalance(id);
        return Result.ok();
    }

    /**
     * 生成试算平衡表并保存快照。
     * <p>
     * 路径放在期初余额控制器下是权宜之计——两者都属于「账套初始化与校验」，
     * 后续可合并到独立的「期末结账」模块。
     */
    @GetMapping("/trial-balance")
    @RequiresPermission("fi:trial:query")
    @OperLog(module = "fi", type = "TRIAL_BALANCE", value = "生成试算平衡表")
    public Result<TrialBalanceVO> trialBalance(@RequestParam String period) {
        return Result.ok(openingBalanceService.generateTrialBalance(period));
    }
}








