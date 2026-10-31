package com.erp.fi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.entity.FiBizAccountRule;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务单据到会计科目的映射规则。
 * <p>
 * 业务单据审核时按 biz_type + rule_key 查此表确定借贷科目，
 * 修改后对新生成的凭证生效。
 */
@RestController
@RequestMapping("/fi/rule")
@RequiredArgsConstructor
public class FiBizAccountRuleController {

    private final com.erp.fi.mapper.FiBizAccountRuleMapper ruleMapper;

    @GetMapping("/page")
    @RequiresPermission("fi:rule:query")
    @OperLog(module = "fi")
    public Result<PageResult<FiBizAccountRule>> page(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String ruleKey,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<FiBizAccountRule> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<FiBizAccountRule>lambdaQuery()
                        .eq(bizType != null && !bizType.isBlank(), FiBizAccountRule::getBizType, bizType)
                        .eq(ruleKey != null && !ruleKey.isBlank(), FiBizAccountRule::getRuleKey, ruleKey)
                        .orderByAsc(FiBizAccountRule::getBizType)
                        .orderByAsc(FiBizAccountRule::getRuleKey));
        PageResult<FiBizAccountRule> result = PageResult.of(page);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    @RequiresPermission("fi:rule:query")
    @OperLog(module = "fi")
    public Result<FiBizAccountRule> detail(@PathVariable Long id) {
        FiBizAccountRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("映射规则不存在");
        }
        return Result.ok(rule);
    }

    @PostMapping
    @RequiresPermission("fi:rule:add")
    @OperLog(module = "fi", type = "CREATE", value = "新增科目映射规则")
    public Result<Long> create(@RequestBody FiBizAccountRule request) {
        request.setId(null);
        checkDuplicated(request, null);
        ruleMapper.insert(request);
        return Result.ok(request.getId());
    }

    @PutMapping
    @RequiresPermission("fi:rule:edit")
    @OperLog(module = "fi", type = "UPDATE", value = "修改科目映射规则")
    public Result<Void> update(@RequestBody FiBizAccountRule request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        checkDuplicated(request, request.getId());
        ruleMapper.updateById(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("fi:rule:remove")
    @OperLog(module = "fi", type = "DELETE", value = "删除科目映射规则")
    public Result<Void> remove(@PathVariable Long id) {
        ruleMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 同一业务类型下规则键唯一，避免取科目时产生歧义。
     */
    private void checkDuplicated(FiBizAccountRule request, Long excludeId) {
        Long count = ruleMapper.selectCount(Wrappers.<FiBizAccountRule>lambdaQuery()
                .eq(FiBizAccountRule::getBizType, request.getBizType())
                .eq(FiBizAccountRule::getRuleKey, request.getRuleKey())
                .ne(excludeId != null, FiBizAccountRule::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("该业务类型下的规则键已存在");
        }
    }
}
