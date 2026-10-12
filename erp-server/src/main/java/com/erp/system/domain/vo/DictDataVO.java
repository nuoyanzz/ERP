package com.erp.system.domain.vo;

import lombok.Data;

/**
 * 字典项展示对象。
 */
@Data
public class DictDataVO {

    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sortNo;
    private String cssClass;
    private String listClass;
    private Integer isDefault;
}








