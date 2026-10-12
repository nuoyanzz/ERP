package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmMo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PmMoMapper extends BaseMapper<PmMo> {

    @Select("""
            SELECT mo_no FROM pm_mo
            WHERE mo_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY mo_no DESC LIMIT 1
            """)
    String selectLastMoNo(@Param("prefix") String prefix);

    @Update("""
            UPDATE pm_mo SET produced_qty = produced_qty + #{qty}
            WHERE id = #{id}
            """)
    int addProducedQty(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}








