package com.erp.pa.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaReturnDetailVO extends PaReturnListVO {

    private Long voucherId;
    private String remark;
    private List<PaReturnEntryVO> entries;
}








