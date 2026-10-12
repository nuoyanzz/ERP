package com.erp.system.service;

import com.erp.system.domain.vo.DictDataVO;

import java.util.List;

/**
 * 数据字典服务。
 */
public interface SysDictService {

    /**
     * 按字典类型查询启用中的字典项。
     */
    List<DictDataVO> listByType(String dictType);

    /**
     * 按字典类型查询全部字典项（含停用）。
     */
    List<DictDataVO> listAllByType(String dictType);

    /**
     * 刷新字典缓存。
     */
    void refreshCache();
}








