package com.erp.wf.integration;

import com.erp.pm.service.PmMoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 生产订单审批监听器。
 * <p>
 * 生产订单审核会按 BOM 展开需求子项，必须在审批通过后才执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PMMoApprovalListener implements BizApprovalListener {

    /** 业务类型，需与 wf_definition.biz_type 一致 */
    public static final String BIZ_TYPE = "PM_MO";

    private final PmMoService moService;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Map<String, Object> vars) {
        log.info("生产订单审批通过，开始执行审核：bizId={}", bizId);
        moService.auditMo(bizId);
    }

    @Override
    public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
        log.info("生产订单审批驳回：bizId={}，原因={}", bizId, reason);
    }
}








