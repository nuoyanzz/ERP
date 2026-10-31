package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.ExchangeRateQueryRequest;
import com.erp.fi.domain.dto.ExchangeRateSaveRequest;
import com.erp.fi.domain.entity.FiExchangeRate;
import com.erp.fi.domain.vo.ExchangeRateVO;
import com.erp.fi.mapper.FiExchangeRateMapper;
import com.erp.fi.service.FiExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiExchangeRateServiceImpl extends ServiceImpl<FiExchangeRateMapper, FiExchangeRate>
        implements FiExchangeRateService {

    @Override
    public PageResult<ExchangeRateVO> pageRates(ExchangeRateQueryRequest request) {
        Page<FiExchangeRate> page = this.page(request.toPage(),
                Wrappers.<FiExchangeRate>lambdaQuery()
                        .like(StringUtils.hasText(request.getCurrency()), FiExchangeRate::getCurrency,
                                request.getCurrency())
                        .eq(StringUtils.hasText(request.getPeriod()), FiExchangeRate::getPeriod,
                                request.getPeriod())
                        .orderByDesc(FiExchangeRate::getPeriod)
                        .orderByAsc(FiExchangeRate::getCurrency));
        List<ExchangeRateVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        Page<ExchangeRateVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ExchangeRateVO getRateDetail(Long id) {
        FiExchangeRate rate = this.getById(id);
        if (rate == null) {
            throw new BizException("汇率不存在");
        }
        return toVO(rate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRate(ExchangeRateSaveRequest request) {
        if (!request.getPeriod().matches("^\\d{4}-\\d{2}$")) {
            throw new BizException("期间格式应为 YYYY-MM");
        }
        if (request.getRate() == null || request.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("汇率必须大于0");
        }
        // 同币别同期间只保留一条，避免调汇时取到不确定的值
        Long duplicated = this.count(Wrappers.<FiExchangeRate>lambdaQuery()
                .eq(FiExchangeRate::getCurrency, request.getCurrency())
                .eq(FiExchangeRate::getPeriod, request.getPeriod())
                .ne(request.getId() != null, FiExchangeRate::getId, request.getId()));
        if (duplicated != null && duplicated > 0) {
            throw new BizException("该币别在该期间已配置汇率");
        }
        FiExchangeRate rate = new FiExchangeRate();
        BeanUtils.copyProperties(request, rate);
        if (rate.getRateType() == null) {
            rate.setRateType(1);
        }
        if (request.getId() == null) {
            this.save(rate);
        } else {
            this.updateById(rate);
        }
        return rate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRate(Long id) {
        this.removeById(id);
    }

    @Override
    public BigDecimal resolveRate(String currency, String period) {
        if (!StringUtils.hasText(currency) || !StringUtils.hasText(period)) {
            return BigDecimal.ONE;
        }
        FiExchangeRate rate = this.getOne(Wrappers.<FiExchangeRate>lambdaQuery()
                .eq(FiExchangeRate::getCurrency, currency)
                .eq(FiExchangeRate::getPeriod, period)
                .last("limit 1"));
        if (rate == null || rate.getRate() == null) {
            return BigDecimal.ONE;
        }
        return rate.getRate();
    }

    private ExchangeRateVO toVO(FiExchangeRate rate) {
        ExchangeRateVO vo = new ExchangeRateVO();
        BeanUtils.copyProperties(rate, vo);
        return vo;
    }
}
