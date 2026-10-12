package com.erp.pa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pa.domain.entity.PaInstockEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaInstockEntryMapper extends BaseMapper<PaInstockEntry> {

    @Update("""
            UPDATE pa_instock_entry SET returned_qty = returned_qty + #{qty}
            WHERE id = #{id}
            """)
    int addReturnedQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








