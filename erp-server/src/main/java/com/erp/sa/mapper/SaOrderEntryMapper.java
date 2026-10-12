package com.erp.sa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sa.domain.entity.SaOrderEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaOrderEntryMapper extends BaseMapper<SaOrderEntry> {

    @Update("""
            UPDATE sa_order_entry SET delivered_qty = delivered_qty + #{qty}
            WHERE id = #{id}
            """)
    int addDeliveredQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








