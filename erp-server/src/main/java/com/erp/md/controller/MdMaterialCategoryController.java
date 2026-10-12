package com.erp.md.controller;

import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MaterialCategorySaveRequest;
import com.erp.md.domain.vo.MaterialCategoryTreeVO;
import com.erp.md.service.MdMaterialCategoryService;
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
@RequestMapping("/md/material-category")
@RequiredArgsConstructor
public class MdMaterialCategoryController {

    private final MdMaterialCategoryService categoryService;

    @GetMapping("/tree")
    @RequiresPermission("md:material-category:query")
    @OperLog(module = "md")
    public Result<List<MaterialCategoryTreeVO>> tree() {
        return Result.ok(categoryService.treeCategories());
    }

    @GetMapping("/{id}")
    @RequiresPermission("md:material-category:query")
    @OperLog(module = "md")
    public Result<MaterialCategoryTreeVO> detail(@PathVariable Long id) {
        return Result.ok(categoryService.getCategoryDetail(id));
    }

    @PostMapping
    @RequiresPermission("md:material-category:add")
    @OperLog(module = "md")
    public Result<Long> create(@Valid @RequestBody MaterialCategorySaveRequest request) {
        return Result.ok(categoryService.createCategory(request));
    }

    @PutMapping
    @RequiresPermission("md:material-category:edit")
    @OperLog(module = "md")
    public Result<Void> update(@Valid @RequestBody MaterialCategorySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        categoryService.updateCategory(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("md:material-category:remove")
    @OperLog(module = "md")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }
}








