package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmPickSaveRequest;
import com.erp.pm.domain.vo.PmPickVO;

public interface PmPickService {

    PageResult<PmPickVO> pagePick(int pageNum, int pageSize, Long moId, Integer status);

    PmPickVO getPickDetail(Long id);

    Long createPick(PmPickSaveRequest request);

    void updatePick(PmPickSaveRequest request);

    void deletePick(Long id);

    void auditPick(Long id);
}








