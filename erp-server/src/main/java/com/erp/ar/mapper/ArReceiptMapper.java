package com.erp.ar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ar.domain.entity.ArReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArReceiptMapper extends BaseMapper<ArReceipt> {

    @Select("""
            SELECT receipt_no FROM ar_receipt
            WHERE receipt_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY receipt_no DESC LIMIT 1
            """)
    String selectLastReceiptNo(@Param("prefix") String prefix);
}








