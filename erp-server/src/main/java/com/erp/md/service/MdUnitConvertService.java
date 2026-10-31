package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.MdUnitConvertQueryRequest;
import com.erp.md.domain.dto.MdUnitConvertSaveRequest;
import com.erp.md.domain.vo.MdUnitConvertVO;

import java.math.BigDecimal;

public interface MdUnitConvertService {

    PageResult<MdUnitConvertVO> pageConvert(MdUnitConvertQueryRequest request);

    MdUnitConvertVO getConvertDetail(Long id);

    Long createConvert(MdUnitConvertSaveRequest request);

    void updateConvert(MdUnitConvertSaveRequest request);

    void deleteConvert(Long id);

    /**
     * 按物料与来源单位查询换算率。
     *
     * @param materialId 物料 ID
     * @param unit       来源单位
     * @return 换算率，未配置时返回 1
     */
    BigDecimal getConvertRate(Long materialId, String unit);
}
