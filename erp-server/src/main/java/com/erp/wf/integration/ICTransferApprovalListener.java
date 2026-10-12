package com.erp.wf.integration;

import com.erp.ic.service.IcTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存调拨单审批监听器。
 * <p>
 * 调拨审核会实际增减库存，必须在审批通过后才执行，因此监听器直接调用业务 Service 的审核方法。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ICTransferApprovalListener implements BizApprovalListener {

    /** 业务类型，需与 wf_definition.biz_type 一致 */
    public static final String BIZ_TYPE = "IC_TRANSFER";

    private final IcTransferService transferService;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Map<String, Object> vars) {
        log.info("库存调拨单审批通过，开始执行审核：bizId={}", bizId);
        transferService.auditTransfer(bizId);
    }

    @Override
    public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
        log.info("库存调拨单审批驳回：bizId={}，原因={}", bizId, reason);
    }
}








