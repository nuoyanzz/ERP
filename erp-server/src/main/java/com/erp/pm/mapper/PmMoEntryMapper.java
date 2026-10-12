package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmMoEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PmMoEntryMapper extends BaseMapper<PmMoEntry> {

    @Update("""
            UPDATE pm_mo_entry SET picked_qty = picked_qty + #{qty}
            WHERE id = #{id}
            """)
    int addPickedQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








