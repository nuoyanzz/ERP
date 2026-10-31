package com.erp.hr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.SocialPolicySaveRequest;
import com.erp.hr.domain.entity.HrSocialPolicy;
import com.erp.hr.domain.vo.SocialPolicyVO;
import com.erp.hr.mapper.HrSocialPolicyMapper;
import com.erp.hr.service.HrSocialPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrSocialPolicyServiceImpl extends ServiceImpl<HrSocialPolicyMapper, HrSocialPolicy>
        implements HrSocialPolicyService {

    @Override
    public List<SocialPolicyVO> listPolicies() {
        return this.list(Wrappers.<HrSocialPolicy>lambdaQuery()
                        .orderByDesc(HrSocialPolicy::getEffectiveDate))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public SocialPolicyVO getPolicyDetail(Long id) {
        HrSocialPolicy policy = this.getById(id);
        if (policy == null) {
            throw new BizException("政策不存在");
        }
        return toVO(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePolicy(SocialPolicySaveRequest request) {
        if (request.getSocialRate() != null
                && request.getSocialRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("社保比例不能为负");
        }
        if (request.getFundRate() != null
                && request.getFundRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("公积金比例不能为负");
        }
        if (request.getTaxThreshold() != null
                && request.getTaxThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("个税起征点不能为负");
        }
        HrSocialPolicy policy = new HrSocialPolicy();
        BeanUtils.copyProperties(request, policy);
        if (policy.getStatus() == null) {
            policy.setStatus(0);
        }
        if (request.getId() == null) {
            this.save(policy);
        } else {
            this.updateById(policy);
        }
        return policy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePolicy(Long id) {
        this.removeById(id);
    }

    @Override
    public HrSocialPolicy resolvePolicy(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        // 取生效日期不晚于目标日期的最新一条
        HrSocialPolicy policy = this.getOne(Wrappers.<HrSocialPolicy>lambdaQuery()
                .le(HrSocialPolicy::getEffectiveDate, target)
                .orderByDesc(HrSocialPolicy::getEffectiveDate)
                .last("limit 1"));
        if (policy != null) {
            return policy;
        }
        // 没有生效的政策时退回最早的一条，保证薪资计算不中断
        return this.getOne(Wrappers.<HrSocialPolicy>lambdaQuery()
                .orderByAsc(HrSocialPolicy::getEffectiveDate)
                .last("limit 1"));
    }

    private SocialPolicyVO toVO(HrSocialPolicy policy) {
        SocialPolicyVO vo = new SocialPolicyVO();
        BeanUtils.copyProperties(policy, vo);
        return vo;
    }
}
