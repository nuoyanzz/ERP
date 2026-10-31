package com.erp.fi.service;

import com.erp.common.api.PageResult;
import com.erp.fi.domain.dto.ExchangeRateQueryRequest;
import com.erp.fi.domain.dto.ExchangeRateSaveRequest;
import com.erp.fi.domain.vo.ExchangeRateVO;

import java.math.BigDecimal;

/**
 * 汇率维护服务。
 */
public interface FiExchangeRateService {

    PageResult<ExchangeRateVO> pageRates(ExchangeRateQueryRequest request);

    ExchangeRateVO getRateDetail(Long id);

    Long saveRate(ExchangeRateSaveRequest request);

    void removeRate(Long id);

    /**
     * 查询指定期间指定币别的汇率；没有配置时返回 1（本币）。
     */
    BigDecimal resolveRate(String currency, String period);
}
