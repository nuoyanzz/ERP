package com.erp.md.service;

import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdPriceSaveRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格表保存校验器。
 * <p>
 * 校验规则与数据库无关，抽取成静态方法后可以脱离 Spring 上下文做单元测试。
 * 服务层在写入前调用 {@link #validate(MdPriceSaveRequest)}。
 */
public final class MdPriceValidator {

    private MdPriceValidator() {
    }

    /**
     * 校验价格保存请求。
     *
     * @throws BizException 校验不通过时抛出，提示信息为中文
     */
    public static void validate(MdPriceSaveRequest request) {
        if (request == null) {
            throw new BizException("价格信息不能为空");
        }
        validatePrice(request.getPrice());
        validateTaxRate(request.getTaxRate());
        validateDateRange(request.getEffectiveDate(), request.getExpiryDate());
    }

    /**
     * 单价必须大于 0。
     * <p>
     * 价格直接参与金额计算，0 或负数会污染应收应付与总账，因此不合法。
     */
    public static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BizException("单价不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("单价必须大于0");
        }
    }

    /**
     * 税率不能为负数。
     */
    public static void validateTaxRate(BigDecimal taxRate) {
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("税率不能为负数");
        }
    }

    /**
     * 价格有效期区间校验。
     * <p>
     * 只有生效日期与失效日期同时存在时才需要比较，失效日期当天仍然有效，
     * 因此允许与生效日期相同，但不允许早于生效日期。
     *
     * @throws BizException 失效日期早于生效日期时抛出
     */
    public static void validateDateRange(LocalDate effectiveDate, LocalDate expiryDate) {
        if (effectiveDate == null || expiryDate == null) {
            return;
        }
        if (expiryDate.isBefore(effectiveDate)) {
            throw new BizException("失效日期不能早于生效日期");
        }
    }
}
