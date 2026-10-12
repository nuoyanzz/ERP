package com.erp.fi.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.FiAccountSaveRequest;
import com.erp.fi.domain.vo.FiAccountTreeVO;
import com.erp.fi.service.FiAccountService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fi/account")
@RequiredArgsConstructor
public class FiAccountController {

    private final FiAccountService accountService;

    @GetMapping("/tree")
    @RequiresPermission("fi:account:query")
    @OperLog(module = "fi")
    public Result<List<FiAccountTreeVO>> tree() {
        return Result.ok(accountService.treeAccounts());
    }

    @GetMapping("/{id}")
    @RequiresPermission("fi:account:query")
    @OperLog(module = "fi")
    public Result<FiAccountTreeVO> detail(@PathVariable Long id) {
        return Result.ok(accountService.getAccountDetail(id));
    }

    @PostMapping
    @RequiresPermission("fi:account:add")
    @OperLog(module = "fi")
    public Result<Long> create(@Valid @RequestBody FiAccountSaveRequest request) {
        return Result.ok(accountService.createAccount(request));
    }

    @PutMapping
    @RequiresPermission("fi:account:edit")
    @OperLog(module = "fi")
    public Result<Void> update(@Valid @RequestBody FiAccountSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        accountService.updateAccount(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("fi:account:remove")
    @OperLog(module = "fi")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return Result.ok();
    }
}








