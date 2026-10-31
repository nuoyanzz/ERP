package com.erp.system.service;

import com.erp.common.api.PageResult;
import com.erp.system.domain.dto.OperLogQueryRequest;
import com.erp.system.domain.entity.SysOperLog;

import java.util.List;

/**
 * 操作日志查询服务。
 */
public interface SysOperLogService {

    PageResult<SysOperLog> pageLogs(OperLogQueryRequest request);

    SysOperLog getLogDetail(Long id);

    List<SysOperLog> listRecent(int limit);
}
