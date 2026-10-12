package com.erp.ic.service;

import com.erp.common.api.PageResult;
import com.erp.ic.domain.dto.IcCheckSaveRequest;
import com.erp.ic.domain.vo.IcCheckDetailVO;
import com.erp.ic.domain.vo.IcCheckListVO;

public interface IcCheckService {

    PageResult<IcCheckListVO> pageCheck(int pageNum, int pageSize, Long warehouseId, Integer status);

    IcCheckDetailVO getCheckDetail(Long id);

    Long createCheck(IcCheckSaveRequest request);

    void updateCheck(IcCheckSaveRequest request);

    void deleteCheck(Long id);

    void auditCheck(Long id);
}








