package com.erp.md.service;

import com.erp.common.api.PageResult;
import com.erp.md.domain.dto.MdPriceQueryRequest;
import com.erp.md.domain.dto.MdPriceSaveRequest;
import com.erp.md.domain.vo.MdPriceVO;

public interface MdPriceService {

    PageResult<MdPriceVO> pagePrice(MdPriceQueryRequest request);

    MdPriceVO getPriceDetail(Long id);

    Long createPrice(MdPriceSaveRequest request);

    void updatePrice(MdPriceSaveRequest request);

    void deletePrice(Long id);
}
