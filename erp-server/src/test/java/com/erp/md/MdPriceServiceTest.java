package com.erp.md;

import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdPriceSaveRequest;
import com.erp.md.domain.entity.MdPrice;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdPriceMapper;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.md.service.impl.MdPriceServiceImpl;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 价格表服务单元测试。
 * <p>
 * 不启动 Spring 上下文，直接用 Mockito 打桩 Mapper，
 * 验证价格保存的两条核心校验规则：单价必须大于 0、有效期区间必须合法。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdPriceServiceTest {

    @Mock
    private MdPriceMapper priceMapper;
    @Mock
    private MdCustomerMapper customerMapper;
    @Mock
    private MdSupplierMapper supplierMapper;
    @Mock
    private MdMaterialMapper materialMapper;

    private MdPriceServiceImpl priceService;

    @BeforeEach
    void setUp() {
        priceService = new MdPriceServiceImpl(customerMapper, supplierMapper, materialMapper);
        // ServiceImpl 的 baseMapper 由 Spring 注入，单元测试中手动塞入桩对象
        ReflectionTestUtils.setField(priceService, "baseMapper", priceMapper);
    }

    private MdPriceSaveRequest validRequest() {
        MdPriceSaveRequest request = new MdPriceSaveRequest();
        request.setPriceNo("P-001");
        request.setPriceName("标准价格");
        request.setMaterialId(1001L);
        request.setPrice(new BigDecimal("10.50"));
        request.setTaxRate(new BigDecimal("13"));
        request.setCurrency("CNY");
        request.setEffectiveDate(LocalDate.of(2026, 1, 1));
        request.setExpiryDate(LocalDate.of(2026, 12, 31));
        request.setStatus(0);
        return request;
    }

    @Test
    @DisplayName("负单价被拒绝")
    void negativePriceRejected() {
        MdPriceSaveRequest request = validRequest();
        request.setPrice(new BigDecimal("-1"));

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("单价必须大于0", ex.getMessage());
        // 校验不通过时不应落库
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("零单价被拒绝")
    void zeroPriceRejected() {
        MdPriceSaveRequest request = validRequest();
        request.setPrice(BigDecimal.ZERO);

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("单价必须大于0", ex.getMessage());
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("空单价被拒绝")
    void nullPriceRejected() {
        MdPriceSaveRequest request = validRequest();
        request.setPrice(null);

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("单价不能为空", ex.getMessage());
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("失效日期早于生效日期被拒绝")
    void invalidDateRangeRejected() {
        MdPriceSaveRequest request = validRequest();
        request.setEffectiveDate(LocalDate.of(2026, 6, 1));
        request.setExpiryDate(LocalDate.of(2026, 5, 31));

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("失效日期不能早于生效日期", ex.getMessage());
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("更新时同样执行校验")
    void updateValidatesToo() {
        MdPriceSaveRequest request = validRequest();
        request.setId(1L);
        request.setPrice(new BigDecimal("-5"));

        MdPrice exist = new MdPrice();
        exist.setId(1L);
        exist.setPriceNo("P-001");
        when(priceMapper.selectById(1L)).thenReturn(exist);

        BizException ex = assertThrows(BizException.class, () -> priceService.updatePrice(request));
        assertEquals("单价必须大于0", ex.getMessage());
        verify(priceMapper, never()).updateById(any(MdPrice.class));
    }

    @Test
    @DisplayName("合法价格可以保存")
    void validPriceAccepted() {
        MdPriceSaveRequest request = validRequest();
        when(priceMapper.selectCount(any())).thenReturn(0L);
        when(priceMapper.insert(any(MdPrice.class))).thenAnswer(invocation -> {
            MdPrice entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });

        Long id = priceService.createPrice(request);

        assertEquals(88L, id);
        verify(priceMapper).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("价格单号重复被拒绝")
    void duplicatePriceNoRejected() {
        MdPriceSaveRequest request = validRequest();
        when(priceMapper.selectCount(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("价格单号已存在", ex.getMessage());
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("只填生效日期时不校验区间")
    void onlyEffectiveDateIsAllowed() {
        MdPriceSaveRequest request = validRequest();
        request.setExpiryDate(null);
        when(priceMapper.selectCount(any())).thenReturn(0L);
        when(priceMapper.insert(any(MdPrice.class))).thenAnswer(invocation -> {
            MdPrice entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });

        assertEquals(99L, priceService.createPrice(request));
        verify(priceMapper).insert(any(MdPrice.class));
    }

    @Test
    @DisplayName("负税率被拒绝")
    void negativeTaxRateRejected() {
        MdPriceSaveRequest request = validRequest();
        request.setTaxRate(new BigDecimal("-1"));

        BizException ex = assertThrows(BizException.class, () -> priceService.createPrice(request));
        assertEquals("税率不能为负数", ex.getMessage());
        verify(priceMapper, never()).insert(any(MdPrice.class));
    }
}
