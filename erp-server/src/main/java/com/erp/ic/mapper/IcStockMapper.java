package com.erp.ic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ic.domain.entity.IcStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface IcStockMapper extends BaseMapper<IcStock> {

    @Select("""
            SELECT * FROM ic_stock
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND batch_no = #{batchNo} AND deleted = 0
            FOR UPDATE
            """)
    IcStock selectForUpdate(@Param("materialId") Long materialId,
                            @Param("warehouseId") Long warehouseId,
                            @Param("batchNo") String batchNo);

    @Select("""
            <script>
            SELECT material_id AS materialId, COALESCE(SUM(qty), 0) AS totalQty FROM ic_stock
            WHERE deleted = 0 AND material_id IN
            <foreach collection="materialIds" item="materialId" open="(" separator="," close=")">#{materialId}</foreach>
            GROUP BY material_id
            </script>
            """)
    List<Map<String, Object>> sumQtyByMaterialIds(@Param("materialIds") List<Long> materialIds);

    @Select("""
            <script>
            SELECT material_id AS materialId, COALESCE(SUM(available_qty), 0) AS availableQty FROM ic_stock
            WHERE deleted = 0 AND material_id IN
            <foreach collection="materialIds" item="materialId" open="(" separator="," close=")">#{materialId}</foreach>
            GROUP BY material_id
            </script>
            """)
    List<Map<String, Object>> sumAvailableByMaterialIds(@Param("materialIds") List<Long> materialIds);
}








