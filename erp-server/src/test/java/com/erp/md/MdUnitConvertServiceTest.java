package com.erp.md;

import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdUnitConvertSaveRequest;
import com.erp.md.domain.entity.MdUnitConvert;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdUnitConvertMapper;
import com.erp.md.service.impl.MdUnitConvertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单位换算服务单元测试。
 * <p>
 * 验证换算率必须大于 0、重复换算关系被拦截，以及未配置换算关系时按 1:1 返回。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdUnitConvertServiceTest {

    @Mock
    private MdUnitConvertMapper unitConvertMapper;
    @Mock
    private MdMaterialMapper materialMapper;

    private MdUnitConvertServiceImpl unitConvertService;

    @BeforeEach
    void setUp() {
        unitConvertService = new MdUnitConvertServiceImpl(materialMapper);
        ReflectionTestUtils.setField(unitConvertService, "baseMapper", unitConvertMapper);
    }

    private MdUnitConvertSaveRequest validRequest() {
        MdUnitConvertSaveRequest request = new MdUnitConvertSaveRequest();
        request.setMaterialId(1001L);
        request.setFromUnit("BOX");
        request.setToUnit("PCS");
        request.setConvertRate(new BigDecimal("24"));
        return request;
    }

    @Test
    @DisplayName("非正换算率被拒绝")
    void nonPositiveRateRejected() {
        MdUnitConvertSaveRequest request = validRequest();
        request.setConvertRate(BigDecimal.ZERO);

        BizException ex = assertThrows(BizException.class, () -> unitConvertService.createConvert(request));
        assertEquals("换算率必须大于0", ex.getMessage());
        verify(unitConvertMapper, never()).insert(any(MdUnitConvert.class));
    }

    @Test
    @DisplayName("负换算率被拒绝")
    void negativeRateRejected() {
        MdUnitConvertSaveRequest request = validRequest();
        request.setConvertRate(new BigDecimal("-2"));

        BizException ex = assertThrows(BizException.class, () -> unitConvertService.createConvert(request));
        assertEquals("换算率必须大于0", ex.getMessage());
        verify(unitConvertMapper, never()).insert(any(MdUnitConvert.class));
    }

    @Test
    @DisplayName("来源单位与目标单位相同被拒绝")
    void sameUnitRejected() {
        MdUnitConvertSaveRequest request = validRequest();
        request.setToUnit("BOX");

        BizException ex = assertThrows(BizException.class, () -> unitConvertService.createConvert(request));
        assertEquals("来源单位与目标单位不能相同", ex.getMessage());
        verify(unitConvertMapper, never()).insert(any(MdUnitConvert.class));
    }

    @Test
    @DisplayName("同物料同来源单位重复被拦截")
    void duplicateFromUnitRejected() {
        MdUnitConvertSaveRequest request = validRequest();
        // 完全相同的组合命中第一条校验
        when(unitConvertMapper.selectCount(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> unitConvertService.createConvert(request));
        assertEquals("该物料的单位换算关系已存在", ex.getMessage());
        verify(unitConvertMapper, never()).insert(any(MdUnitConvert.class));
    }

    @Test
    @DisplayName("未配置换算关系时换算率为 1")
    void defaultRateIsOne() {
        when(unitConvertMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        BigDecimal rate = unitConvertService.getConvertRate(1001L, "BOX");

        assertEquals(0, BigDecimal.ONE.compareTo(rate));
    }

    @Test
    @DisplayName("已配置换算关系时返回配置值")
    void configuredRateReturned() {
        MdUnitConvert convert = new MdUnitConvert();
        convert.setId(1L);
        convert.setMaterialId(1001L);
        convert.setFromUnit("BOX");
        convert.setToUnit("PCS");
        convert.setConvertRate(new BigDecimal("12.5"));
        when(unitConvertMapper.selectOne(any(), anyBoolean())).thenReturn(convert);

        BigDecimal rate = unitConvertService.getConvertRate(1001L, "BOX");

        assertEquals(0, new BigDecimal("12.5").compareTo(rate));
    }
}
