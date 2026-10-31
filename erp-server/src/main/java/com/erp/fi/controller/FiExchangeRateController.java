package com.erp.fi.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.ExchangeRateQueryRequest;
import com.erp.fi.domain.dto.ExchangeRateSaveRequest;
import com.erp.fi.domain.vo.ExchangeRateVO;
import com.erp.fi.service.FiExchangeRateService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇率维护。用于外币业务的折算与期末调汇。
 */
@RestController
@RequestMapping("/fi/rate")
@RequiredArgsConstructor
public class FiExchangeRateController {

    private final FiExchangeRateService rateService;

    @GetMapping("/page")
    @RequiresPermission("fi:rate:query")
    @OperLog(module = "fi")
    public Result<PageResult<ExchangeRateVO>> page(ExchangeRateQueryRequest request) {
        return Result.ok(rateService.pageRates(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("fi:rate:query")
    @OperLog(module = "fi")
    public Result<ExchangeRateVO> detail(@PathVariable Long id) {
        return Result.ok(rateService.getRateDetail(id));
    }

    @PostMapping
    @RequiresPermission("fi:rate:add")
    @OperLog(module = "fi", type = "CREATE", value = "新增汇率")
    public Result<Long> create(@Valid @RequestBody ExchangeRateSaveRequest request) {
        request.setId(null);
        return Result.ok(rateService.saveRate(request));
    }

    @PutMapping
    @RequiresPermission("fi:rate:edit")
    @OperLog(module = "fi", type = "UPDATE", value = "修改汇率")
    public Result<Void> update(@Valid @RequestBody ExchangeRateSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        rateService.saveRate(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("fi:rate:remove")
    @OperLog(module = "fi", type = "DELETE", value = "删除汇率")
    public Result<Void> remove(@PathVariable Long id) {
        rateService.removeRate(id);
        return Result.ok();
    }
}
