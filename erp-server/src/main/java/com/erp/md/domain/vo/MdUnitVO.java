package com.erp.md.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MdUnitVO {

    private Long id;
    private String unitCode;
    private String unitName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
