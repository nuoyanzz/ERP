package com.erp.wf.integration;

import com.erp.ic.service.IcCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存盘点单审批监听器。
 * <p>
 * 盘点审核会按实盘数调整库存并生成盘盈盘亏凭证，必须在审批通过后才执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ICCheckApprovalListener implements BizApprovalListener {

    /** 业务类型，需与 wf_definition.biz_type 一致 */
    public static final String BIZ_TYPE = "IC_CHECK";

    private final IcCheckService checkService;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Map<String, Object> vars) {
        log.info("库存盘点单审批通过，开始执行审核：bizId={}", bizId);
        checkService.auditCheck(bizId);
    }

    @Override
    public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
        log.info("库存盘点单审批驳回：bizId={}，原因={}", bizId, reason);
    }
}








