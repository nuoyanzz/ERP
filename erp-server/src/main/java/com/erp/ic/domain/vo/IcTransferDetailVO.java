package com.erp.ic.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IcTransferDetailVO extends IcTransferListVO {

    private String remark;
    private List<IcTransferEntryVO> entries;
}








