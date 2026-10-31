package com.erp.ap.controller;

import com.erp.ap.domain.vo.ApAgingVO;
import com.erp.ap.service.ApAgingService;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 应付账龄分析。
 */
@RestController
@RequestMapping("/ap/aging")
@RequiredArgsConstructor
public class ApAgingController {

    private final ApAgingService agingService;

    @GetMapping
    @RequiresPermission("ap:aging:query")
    @OperLog(module = "ap")
    public Result<List<ApAgingVO>> aging(@RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return Result.ok(agingService.aging(asOfDate));
    }
}













