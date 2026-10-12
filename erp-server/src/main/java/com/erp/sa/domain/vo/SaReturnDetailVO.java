package com.erp.sa.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaReturnDetailVO extends SaReturnListVO {

    private Long voucherId;
    private String remark;
    private List<SaReturnEntryVO> entries;
}








