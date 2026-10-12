package com.erp.fi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VoucherVO {

    private Long id;
    private String voucherNo;
    private String word;
    private LocalDate voucherDate;
    private String period;
    private Integer attachCount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Integer status;
    private Integer redFlush;
    private Long sourceVoucherId;
    private String summary;
    private Long submitBy;
    private LocalDateTime submitTime;
    private Long auditBy;
    private LocalDateTime auditTime;
    private Long postBy;
    private LocalDateTime postTime;
    private LocalDateTime createTime;
}








