package com.erp.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 所属字典类型 */
    private String dictType;
    /** 显示标签 */
    private String dictLabel;
    /** 存储值 */
    private String dictValue;
    private Integer sortNo;
    /** 标签样式类，如 success / warning / danger */
    private String cssClass;
    /** 列表展示样式类 */
    private String listClass;
    /** 是否默认选中 */
    private Integer isDefault;
    private Integer status;
    private String remark;
}








