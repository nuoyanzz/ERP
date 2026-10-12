package com.erp.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import com.erp.system.domain.entity.SysDictData;
import com.erp.system.domain.entity.SysDictType;
import com.erp.system.domain.vo.DictDataVO;
import com.erp.system.mapper.SysDictDataMapper;
import com.erp.system.mapper.SysDictTypeMapper;
import com.erp.system.service.SysDictService;
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
 * 数据字典管理。
 */
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysDictService dictService;

    /**
     * 供前端下拉框读取字典项。登录用户均可访问。
     */
    @GetMapping("/data/{dictType}")
    public Result<List<DictDataVO>> listData(@PathVariable String dictType) {
        return Result.ok(dictService.listByType(dictType));
    }

    @GetMapping("/types")
    @RequiresPermission("sys:dict:query")
    @OperLog(module = "system")
    public Result<List<SysDictType>> listTypes() {
        return Result.ok(dictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getStatus, 0)
                .orderByAsc(SysDictType::getDictType)));
    }

    @GetMapping("/data/all/{dictType}")
    @RequiresPermission("sys:dict:query")
    @OperLog(module = "system")
    public Result<List<DictDataVO>> listAllData(@PathVariable String dictType) {
        return Result.ok(dictService.listAllByType(dictType));
    }

    @PostMapping("/type")
    @RequiresPermission("sys:dict:add")
    @OperLog(module = "system", type = "CREATE", value = "新增字典类型")
    public Result<Long> createType(@RequestBody SysDictType request) {
        if (request.getDictType() == null || request.getDictType().isBlank()) {
            throw new BizException("字典编码不能为空");
        }
        Long count = dictTypeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, request.getDictType()));
        if (count != null && count > 0) {
            throw new BizException("字典编码已存在");
        }
        if (request.getStatus() == null) {
            request.setStatus(0);
        }
        dictTypeMapper.insert(request);
        dictService.refreshCache();
        return Result.ok(request.getId());
    }

    @PutMapping("/type")
    @RequiresPermission("sys:dict:edit")
    @OperLog(module = "system", type = "UPDATE", value = "修改字典类型")
    public Result<Void> updateType(@RequestBody SysDictType request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        dictTypeMapper.updateById(request);
        dictService.refreshCache();
        return Result.ok();
    }

    @DeleteMapping("/type/{id}")
    @RequiresPermission("sys:dict:remove")
    @OperLog(module = "system", type = "DELETE", value = "删除字典类型")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictTypeMapper.deleteById(id);
        dictService.refreshCache();
        return Result.ok();
    }

    @PostMapping("/data")
    @RequiresPermission("sys:dict:add")
    @OperLog(module = "system", type = "CREATE", value = "新增字典项")
    public Result<Long> createData(@RequestBody SysDictData request) {
        if (request.getDictType() == null || request.getDictType().isBlank()) {
            throw new BizException("字典类型不能为空");
        }
        if (request.getStatus() == null) {
            request.setStatus(0);
        }
        if (request.getIsDefault() == null) {
            request.setIsDefault(0);
        }
        dictDataMapper.insert(request);
        dictService.refreshCache();
        return Result.ok(request.getId());
    }

    @PutMapping("/data")
    @RequiresPermission("sys:dict:edit")
    @OperLog(module = "system", type = "UPDATE", value = "修改字典项")
    public Result<Void> updateData(@RequestBody SysDictData request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        dictDataMapper.updateById(request);
        dictService.refreshCache();
        return Result.ok();
    }

    @DeleteMapping("/data/{id}")
    @RequiresPermission("sys:dict:remove")
    @OperLog(module = "system", type = "DELETE", value = "删除字典项")
    public Result<Void> deleteData(@PathVariable Long id) {
        dictDataMapper.deleteById(id);
        dictService.refreshCache();
        return Result.ok();
    }

    /**
     * 刷新字典缓存。
     */
    @PostMapping("/refresh")
    @RequiresPermission("sys:dict:edit")
    @OperLog(module = "system", type = "REFRESH", value = "刷新字典缓存")
    public Result<Void> refresh() {
        dictService.refreshCache();
        return Result.ok();
    }
}








