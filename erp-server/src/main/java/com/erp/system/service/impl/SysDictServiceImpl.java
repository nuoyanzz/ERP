package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.system.domain.entity.SysDictData;
import com.erp.system.domain.vo.DictDataVO;
import com.erp.system.mapper.SysDictDataMapper;
import com.erp.system.service.SysDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现。
 * <p>
 * 字典项变更频率低但读取频繁，进程内缓存一份；管理端维护字典后主动刷新缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictDataMapper dictDataMapper;

    /** 字典缓存：dictType → 字典项列表 */
    private final Map<String, List<DictDataVO>> cache = new ConcurrentHashMap<>();

    @Override
    public List<DictDataVO> listByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return Collections.emptyList();
        }
        return cache.computeIfAbsent(dictType, this::loadEnabled);
    }

    @Override
    public List<DictDataVO> listAllByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return Collections.emptyList();
        }
        return dictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                        .eq(SysDictData::getDictType, dictType)
                        .orderByAsc(SysDictData::getSortNo))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void refreshCache() {
        cache.clear();
        log.info("数据字典缓存已刷新");
    }

    private List<DictDataVO> loadEnabled(String dictType) {
        return dictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                        .eq(SysDictData::getDictType, dictType)
                        .eq(SysDictData::getStatus, 0)
                        .orderByAsc(SysDictData::getSortNo))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private DictDataVO toVO(SysDictData data) {
        DictDataVO vo = new DictDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }
}








