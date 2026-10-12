package com.erp.ic.service;

import com.erp.common.api.PageResult;
import com.erp.ic.domain.dto.StockQueryRequest;
import com.erp.ic.domain.dto.TransactionQueryRequest;
import com.erp.ic.domain.vo.IcStockVO;
import com.erp.ic.domain.vo.IcTransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IcStockService {

    PageResult<IcStockVO> pageStock(StockQueryRequest request);

    PageResult<IcTransactionVO> pageTransaction(TransactionQueryRequest request);

    void increaseStock(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                       String tranType, Long bizId, String bizNo, LocalDate bizDate, String remark);

    void decreaseStock(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                       String tranType, Long bizId, String bizNo, LocalDate bizDate, String remark);
}








