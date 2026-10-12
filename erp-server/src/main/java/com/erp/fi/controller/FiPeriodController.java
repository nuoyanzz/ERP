package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.fi.domain.dto.FiPeriodSaveRequest;
import com.erp.fi.domain.vo.FiPeriodVO;
import com.erp.fi.service.FiPeriodService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fi/period")
@RequiredArgsConstructor
public class FiPeriodController {

    private final FiPeriodService periodService;

    @GetMapping("/list")
    @RequiresPermission("fi:period:query")
    @OperLog(module = "fi")
    public Result<List<FiPeriodVO>> list(@RequestParam(required = false) Integer periodYear) {
        return Result.ok(periodService.listByYear(periodYear));
    }

    @GetMapping("/{id}")
    @RequiresPermission("fi:period:query")
    @OperLog(module = "fi")
    public Result<FiPeriodVO> detail(@PathVariable Long id) {
        return Result.ok(periodService.getPeriodDetail(id));
    }

    @PostMapping("/generate")
    @RequiresPermission("fi:period:add")
    @OperLog(module = "fi")
    public Result<Integer> generate(@Valid @RequestBody FiPeriodSaveRequest request) {
        return Result.ok(periodService.generateYearPeriods(request));
    }

    @PutMapping("/{id}/close")
    @RequiresPermission("fi:period:close")
    @OperLog(module = "fi")
    public Result<Void> close(@PathVariable Long id) {
        periodService.closePeriod(id);
        return Result.ok();
    }

    @PutMapping("/{id}/open")
    @RequiresPermission("fi:period:open")
    @OperLog(module = "fi")
    public Result<Void> open(@PathVariable Long id) {
        periodService.openPeriod(id);
        return Result.ok();
    }
}








