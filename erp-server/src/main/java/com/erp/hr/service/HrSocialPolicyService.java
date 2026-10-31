package com.erp.hr.service;

import com.erp.hr.domain.dto.SocialPolicySaveRequest;
import com.erp.hr.domain.entity.HrSocialPolicy;
import com.erp.hr.domain.vo.SocialPolicyVO;

import java.util.List;

/**
 * 社保个税政策服务。
 */
public interface HrSocialPolicyService {

    List<SocialPolicyVO> listPolicies();

    SocialPolicyVO getPolicyDetail(Long id);

    Long savePolicy(SocialPolicySaveRequest request);

    void removePolicy(Long id);

    /**
     * 取指定日期生效的政策；找不到时取最新一条。
     */
    HrSocialPolicy resolvePolicy(java.time.LocalDate date);
}
