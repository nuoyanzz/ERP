package com.erp.wf.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务审批回调注册表。
 * <p>
 * Spring 启动时自动收集所有 {@link BizApprovalListener} 实现，按 bizType 建立索引，
 * 流程结束时据此分发回调。业务模块只要新增一个监听器 Bean 即可接入，无需改动引擎。
 */
@Slf4j
@Component
public class ApprovalCallbackRegistry {

    private final Map<String, BizApprovalListener> listeners = new HashMap<>();

    public ApprovalCallbackRegistry(List<BizApprovalListener> listenerList) {
        if (listenerList == null) {
            return;
        }
        for (BizApprovalListener listener : listenerList) {
            String bizType = listener.bizType();
            if (bizType == null || bizType.isBlank()) {
                log.warn("忽略 bizType 为空的审批监听器：{}", listener.getClass().getName());
                continue;
            }
            BizApprovalListener exist = listeners.put(bizType, listener);
            if (exist != null) {
                log.warn("业务类型 {} 存在多个审批监听器，{} 覆盖 {}",
                        bizType, listener.getClass().getSimpleName(), exist.getClass().getSimpleName());
            }
        }
    }

    /**
     * 查找指定业务类型的监听器。
     */
    public BizApprovalListener find(String bizType) {
        if (bizType == null) {
            return null;
        }
        return listeners.get(bizType);
    }

    /**
     * 是否已注册该业务类型。
     */
    public boolean supports(String bizType) {
        return find(bizType) != null;
    }
}








