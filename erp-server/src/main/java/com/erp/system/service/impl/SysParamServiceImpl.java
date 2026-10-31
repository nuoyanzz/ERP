package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.system.domain.dto.SysParamQueryRequest;
import com.erp.system.domain.dto.SysParamSaveRequest;
import com.erp.system.domain.entity.SysParam;
import com.erp.system.domain.vo.SysParamVO;
import com.erp.system.mapper.SysParamMapper;
import com.erp.system.service.SysParamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 系统参数服务实现。
 * <p>
 * 参数值按 {@code param_type} 校验：STRING 不做限制，NUMBER 必须是数值，
 * BOOLEAN 必须是 true/false，JSON 必须是合法 JSON。校验不通过时给出中文提示，
 * 避免脏数据在业务代码里才暴露。
 */
@Service
@RequiredArgsConstructor
public class SysParamServiceImpl extends ServiceImpl<SysParamMapper, SysParam> implements SysParamService {

    /** 参数类型：字符串 */
    public static final String TYPE_STRING = "STRING";
    /** 参数类型：数值 */
    public static final String TYPE_NUMBER = "NUMBER";
    /** 参数类型：布尔 */
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    /** 参数类型：JSON */
    public static final String TYPE_JSON = "JSON";

    private static final int STATUS_ENABLED = 0;

    private final ObjectMapper objectMapper;

    @Override
    public PageResult<SysParamVO> pageParam(SysParamQueryRequest request) {
        Page<SysParam> page = this.page(request.toPage(), Wrappers.<SysParam>lambdaQuery()
                .like(StringUtils.hasText(request.getParamKey()), SysParam::getParamKey, request.getParamKey())
                .like(StringUtils.hasText(request.getParamName()), SysParam::getParamName, request.getParamName())
                .eq(StringUtils.hasText(request.getParamType()), SysParam::getParamType, request.getParamType())
                .eq(request.getStatus() != null, SysParam::getStatus, request.getStatus())
                .orderByDesc(SysParam::getId));
        List<SysParamVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<SysParamVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<SysParamVO> listEnabledParams() {
        return this.list(Wrappers.<SysParam>lambdaQuery()
                        .eq(SysParam::getStatus, STATUS_ENABLED)
                        .orderByAsc(SysParam::getId))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public SysParamVO getParamDetail(Long id) {
        SysParam param = this.getById(id);
        if (param == null) {
            throw new BizException("系统参数不存在");
        }
        return toVO(param);
    }

    @Override
    public Long createParam(SysParamSaveRequest request) {
        checkKeyUnique(request.getParamKey(), null);
        validateValue(request.getParamType(), request.getParamValue());
        SysParam param = new SysParam();
        BeanUtils.copyProperties(request, param);
        if (param.getStatus() == null) {
            param.setStatus(STATUS_ENABLED);
        }
        if (!StringUtils.hasText(param.getParamType())) {
            param.setParamType(TYPE_STRING);
        }
        this.save(param);
        return param.getId();
    }

    @Override
    public void updateParam(SysParamSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SysParam exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("系统参数不存在");
        }
        checkKeyUnique(request.getParamKey(), request.getId());
        validateValue(request.getParamType(), request.getParamValue());
        SysParam param = new SysParam();
        BeanUtils.copyProperties(request, param);
        this.updateById(param);
    }

    @Override
    public void deleteParam(Long id) {
        SysParam exist = this.getById(id);
        if (exist == null) {
            throw new BizException("系统参数不存在");
        }
        this.removeById(id);
    }

    @Override
    public String getParamValue(String paramKey) {
        if (!StringUtils.hasText(paramKey)) {
            return "";
        }
        SysParam param = this.getOne(Wrappers.<SysParam>lambdaQuery()
                .eq(SysParam::getParamKey, paramKey)
                .eq(SysParam::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        if (param == null || !StringUtils.hasText(param.getParamValue())) {
            return "";
        }
        return param.getParamValue();
    }

    /**
     * 参数键唯一性校验。
     */
    private void checkKeyUnique(String paramKey, Long excludeId) {
        Long count = this.count(Wrappers.<SysParam>lambdaQuery()
                .eq(SysParam::getParamKey, paramKey)
                .ne(excludeId != null, SysParam::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("参数键已存在");
        }
    }

    /**
     * 按参数类型校验参数值的格式。
     */
    private void validateValue(String paramType, String paramValue) {
        if (!StringUtils.hasText(paramType) || !StringUtils.hasText(paramValue)) {
            return;
        }
        switch (paramType) {
            case TYPE_NUMBER -> {
                try {
                    new BigDecimal(paramValue.trim());
                } catch (NumberFormatException e) {
                    throw new BizException("参数类型为 NUMBER，参数值必须是数值：" + paramValue);
                }
            }
            case TYPE_BOOLEAN -> {
                String normalized = paramValue.trim().toLowerCase(Locale.ROOT);
                if (!"true".equals(normalized) && !"false".equals(normalized)) {
                    throw new BizException("参数类型为 BOOLEAN，参数值必须是 true 或 false：" + paramValue);
                }
            }
            case TYPE_JSON -> {
                try {
                    objectMapper.readTree(paramValue);
                } catch (Exception e) {
                    throw new BizException("参数类型为 JSON，参数值必须是合法的 JSON：" + paramValue);
                }
            }
            default -> {
                // STRING 不做格式限制
            }
        }
    }

    private SysParamVO toVO(SysParam param) {
        SysParamVO vo = new SysParamVO();
        BeanUtils.copyProperties(param, vo);
        return vo;
    }
}













