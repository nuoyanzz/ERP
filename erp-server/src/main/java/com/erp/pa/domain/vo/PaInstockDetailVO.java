package com.erp.pa.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaInstockDetailVO extends PaInstockListVO {

    private String remark;
    private Long voucherId;
    private List<PaInstockEntryVO> entries;
}








