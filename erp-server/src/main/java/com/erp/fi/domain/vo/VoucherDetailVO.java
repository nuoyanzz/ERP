package com.erp.fi.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VoucherDetailVO extends VoucherVO {

    private List<VoucherEntryVO> entries;
}








