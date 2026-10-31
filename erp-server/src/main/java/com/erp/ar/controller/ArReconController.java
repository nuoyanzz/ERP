package com.erp.ar.controller;

import com.erp.ar.domain.vo.ArReconVO;
import com.erp.ar.service.ArReconService;
import com.erp.common.api.Result;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应收往来对账。
 */
@RestController
@RequestMapping("/ar/recon")
@RequiredArgsConstructor
public class ArReconController {

    private final ArReconService reconService;

    @GetMapping
    @RequiresPermission("ar:recon:query")
    @OperLog(module = "ar")
    public Result<ArReconVO> recon(@RequestParam Long customerId) {
        return Result.ok(reconService.recon(customerId));
    }
}













