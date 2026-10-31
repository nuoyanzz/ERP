package com.erp.md.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MdPriceVO {

    private Long id;
    private String priceNo;
    private String priceName;
    private Long customerId;
    private Long supplierId;
    private Long materialId;
    private BigDecimal price;
    private BigDecimal taxRate;
    private String currency;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    /** 客户名称，由 md_customer 带出 */
    private String customerName;

    /** 供应商名称，由 md_supplier 带出 */
    private String supplierName;

    /** 物料编码，由 md_material 带出 */
    private String materialCode;

    /** 物料名称，由 md_material 带出 */
    private String materialName;
}
