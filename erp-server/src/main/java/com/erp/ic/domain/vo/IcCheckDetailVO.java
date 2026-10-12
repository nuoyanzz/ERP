package com.erp.ic.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IcCheckDetailVO extends IcCheckListVO {

    private Long voucherId;
    private String remark;
    private List<IcCheckEntryVO> entries;
}








