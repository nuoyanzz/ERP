package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 汇率。用于外币业务的折算与期末调汇。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_exchange_rate")
public class FiExchangeRate extends BaseEntity {

    /** 币别，如 USD */
    private String currency;
    /** 期间，格式 YYYY-MM */
    private String period;
    /** 对本币汇率 */
    private BigDecimal rate;
    /** 汇率类型：1 期间汇率 2 调整汇率 */
    private Integer rateType;
}
