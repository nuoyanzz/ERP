package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.OperLogQueryRequest;
import com.erp.system.domain.entity.SysOperLog;
import com.erp.system.mapper.SysOperLogMapper;
import com.erp.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>
        implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> pageLogs(OperLogQueryRequest request) {
        Page<SysOperLog> page = this.page(request.toPage(),
                Wrappers.<SysOperLog>lambdaQuery()
                        .eq(StringUtils.hasText(request.getModule()), SysOperLog::getModule,
                                request.getModule())
                        .eq(StringUtils.hasText(request.getOperType()), SysOperLog::getOperType,
                                request.getOperType())
                        .like(StringUtils.hasText(request.getOperUsername()), SysOperLog::getOperUsername,
                                request.getOperUsername())
                        .eq(request.getSuccess() != null, SysOperLog::getSuccess, request.getSuccess())
                        .orderByDesc(SysOperLog::getId));
        return PageResult.of(page);
    }

    @Override
    public SysOperLog getLogDetail(Long id) {
        SysOperLog log = this.getById(id);
        if (log == null) {
            throw new BizException("日志不存在");
        }
        return log;
    }

    @Override
    public List<SysOperLog> listRecent(int limit) {
        int size = limit <= 0 ? 20 : Math.min(limit, 200);
        return this.list(Wrappers.<SysOperLog>lambdaQuery()
                .orderByDesc(SysOperLog::getId)
                .last("limit " + size));
    }
}
