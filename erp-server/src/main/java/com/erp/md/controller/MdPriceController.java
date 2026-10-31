package com.erp.md.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.md.domain.dto.MdPriceQueryRequest;
import com.erp.md.domain.dto.MdPriceSaveRequest;
import com.erp.md.domain.vo.MdPriceVO;
import com.erp.md.service.MdPriceService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/md/price")
@RequiredArgsConstructor
public class MdPriceController {

    private final MdPriceService priceService;

    @GetMapping("/page")
    @RequiresPermission("md:price:query")
    @OperLog(module = "md")
    public Result<PageResult<MdPriceVO>> page(MdPriceQueryRequest request) {
        return Result.ok(priceService.pagePrice(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:price:query")
    @OperLog(module = "md")
    public Result<MdPriceVO> detail(@PathVariable Long id) {
        return Result.ok(priceService.getPriceDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:price:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody MdPriceSaveRequest request) {
        return Result.ok(priceService.createPrice(request));
    }

    @PutMapping
    @RequiresPermission("md:price:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody MdPriceSaveRequest request) {
        priceService.updatePrice(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:price:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        priceService.deletePrice(id);
        return Result.ok();
    }
}
