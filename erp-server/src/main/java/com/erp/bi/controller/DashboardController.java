package com.erp.bi.controller;

import com.erp.bi.domain.vo.DashboardVO;
import com.erp.bi.service.DashboardService;
import com.erp.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bi/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardVO> overview() {
        return Result.ok(dashboardService.getDashboard());
    }
}








