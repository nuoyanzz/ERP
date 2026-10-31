package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计量单位主数据。
 * <p>
 * 单位编码是业务单据（物料、库存、价格）引用计量单位的唯一依据，
 * 因此在数据库层通过生成列保证了唯一性，应用层同样做一次校验。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_unit")
public class MdUnit extends BaseEntity {

    /** 单位编码，如 PCS、KG */
    private String unitCode;

    /** 单位名称，如 个、千克 */
    private String unitName;

    /** 状态：0 启用，1 停用 */
    private Integer status;

    private String remark;
}
