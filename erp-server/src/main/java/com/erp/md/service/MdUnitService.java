package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.MdUnitQueryRequest;
import com.erp.md.domain.dto.MdUnitSaveRequest;
import com.erp.md.domain.vo.MdUnitVO;

public interface MdUnitService {

    PageResult<MdUnitVO> pageUnit(MdUnitQueryRequest request);

    MdUnitVO getUnitDetail(Long id);

    Long createUnit(MdUnitSaveRequest request);

    void updateUnit(MdUnitSaveRequest request);

    void deleteUnit(Long id);
}
