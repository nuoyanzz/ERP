package com.erp.pa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pa.domain.entity.PaOrderEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaOrderEntryMapper extends BaseMapper<PaOrderEntry> {

    @Update("""
            UPDATE pa_order_entry SET received_qty = received_qty + #{qty}
            WHERE id = #{id}
            """)
    int addReceivedQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








