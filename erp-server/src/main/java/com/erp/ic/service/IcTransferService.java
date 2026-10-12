package com.erp.ic.service;

import com.erp.common.api.PageResult;
import com.erp.ic.domain.dto.IcTransferSaveRequest;
import com.erp.ic.domain.vo.IcTransferDetailVO;
import com.erp.ic.domain.vo.IcTransferListVO;

public interface IcTransferService {

    PageResult<IcTransferListVO> pageTransfer(int pageNum, int pageSize, Long fromWarehouseId, Long toWarehouseId, Integer status);

    IcTransferDetailVO getTransferDetail(Long id);

    Long createTransfer(IcTransferSaveRequest request);

    void updateTransfer(IcTransferSaveRequest request);

    void deleteTransfer(Long id);

    void auditTransfer(Long id);
}








