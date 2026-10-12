package com.erp.fi.service;

import com.erp.fi.domain.dto.FiPeriodSaveRequest;
import com.erp.fi.domain.vo.FiPeriodVO;

import java.util.List;

public interface FiPeriodService {

    List<FiPeriodVO> listByYear(Integer periodYear);

    FiPeriodVO getPeriodDetail(Long id);

    int generateYearPeriods(FiPeriodSaveRequest request);

    void closePeriod(Long id);

    void openPeriod(Long id);
}








