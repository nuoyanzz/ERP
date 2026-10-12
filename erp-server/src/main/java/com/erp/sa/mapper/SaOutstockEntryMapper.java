package com.erp.sa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sa.domain.entity.SaOutstockEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaOutstockEntryMapper extends BaseMapper<SaOutstockEntry> {

    @Update("""
            UPDATE sa_outstock_entry SET returned_qty = returned_qty + #{qty}
            WHERE id = #{id}
            """)
    int addReturnedQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








