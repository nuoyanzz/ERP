package com.erp.fi.service;

import com.erp.common.api.PageResult;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.dto.VoucherQueryRequest;
import com.erp.fi.domain.dto.VoucherSaveRequest;
import com.erp.fi.domain.vo.VoucherDetailVO;
import com.erp.fi.domain.vo.VoucherVO;

import java.time.LocalDate;
import java.util.List;

public interface FiVoucherService {

    PageResult<VoucherVO> pageVoucher(VoucherQueryRequest request);

    VoucherDetailVO getVoucherDetail(Long id);

    Long createVoucher(VoucherSaveRequest request);

    void updateVoucher(VoucherSaveRequest request);

    void deleteVoucher(Long id);

    void submitVoucher(Long id);

    void auditVoucher(Long id);

    /** 反审核：把已审核凭证退回已提交状态 */
    void unauditVoucher(Long id);

    void postVoucher(Long id);

    /** 反记账：撤销已记账凭证，使其退出账簿 */
    void unpostVoucher(Long id);

    /** 作废凭证，保留痕迹但不再参与账簿计算 */
    void voidVoucher(Long id, String reason);

    Long redFlushVoucher(Long id);

    Long generateBizVoucher(String bizType, LocalDate voucherDate, String summary, List<VoucherLine> lines);
}








