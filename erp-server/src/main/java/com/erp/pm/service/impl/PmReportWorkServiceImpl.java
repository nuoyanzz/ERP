package com.erp.pm.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.service.FiVoucherService;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.pm.domain.dto.PmReportWorkSaveRequest;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmReportWork;
import com.erp.pm.domain.entity.PmRoutingOperation;
import com.erp.pm.domain.vo.PmReportWorkVO;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.mapper.PmReportWorkMapper;
import com.erp.pm.mapper.PmRoutingOperationMapper;
import com.erp.pm.service.PmCostService;
import com.erp.pm.service.PmReportWorkService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmReportWorkServiceImpl extends ServiceImpl<PmReportWorkMapper, PmReportWork>
        implements PmReportWorkService {

    private static final String NO_PREFIX = "RW-";
    private static final String VOUCHER_BIZ_TYPE = "PM_REPORT";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int SCALE = 4;

    private final PmMoMapper moMapper;
    private final PmRoutingOperationMapper operationMapper;
    private final PmReportWorkMapper reportWorkMapper;
    private final MdMaterialMapper materialMapper;
    private final HrEmployeeMapper employeeMapper;
    private final PmCostService costService;
    private final FiVoucherService voucherService;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<PmReportWorkVO> pageReport(int pageNum, int pageSize, Long moId, Integer status, String reportNo) {
        Page<PmReportWork> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmReportWork>lambdaQuery()
                .eq(moId != null, PmReportWork::getMoId, moId)
                .eq(status != null, PmReportWork::getStatus, status)
                .like(StringUtils.hasText(reportNo), PmReportWork::getReportNo, reportNo)
                .orderByDesc(PmReportWork::getId));
        List<PmReportWorkVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmReportWorkVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmReportWorkVO getReportDetail(Long id) {
        PmReportWork report = this.getById(id);
        if (report == null) {
            throw new BizException("报工单不存在");
        }
        return toVO(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(PmReportWorkSaveRequest request) {
        PmMo mo = moMapper.selectById(request.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (mo.getStatus() == null || (mo.getStatus() != PmMo.STATUS_AUDITED && mo.getStatus() != PmMo.STATUS_PROCESSING)) {
            throw new BizException("生产订单未下达，不能报工");
        }
        validateOperation(request.getOperationId());
        validateQty(request);
        PmReportWork report = new PmReportWork();
        report.setReportNo(nextNo(request.getReportDate()));
        report.setMoId(request.getMoId());
        report.setOperationId(request.getOperationId());
        report.setEmployeeId(request.getEmployeeId());
        report.setReportDate(request.getReportDate());
        report.setQty(request.getQty());
        report.setQualifiedQty(resolveQualifiedQty(request));
        report.setScrapQty(request.getScrapQty() == null ? BigDecimal.ZERO : request.getScrapQty());
        report.setWorkHours(request.getWorkHours());
        report.setStatus(PmReportWork.STATUS_DRAFT);
        report.setRemark(request.getRemark());
        this.save(report);
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(PmReportWorkSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmReportWork exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("报工单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmReportWork.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的报工单才能修改");
        }
        validateOperation(request.getOperationId());
        validateQty(request);
        PmReportWork report = new PmReportWork();
        report.setId(request.getId());
        report.setMoId(request.getMoId());
        report.setOperationId(request.getOperationId());
        report.setEmployeeId(request.getEmployeeId());
        report.setReportDate(request.getReportDate());
        report.setQty(request.getQty());
        report.setQualifiedQty(resolveQualifiedQty(request));
        report.setScrapQty(request.getScrapQty() == null ? BigDecimal.ZERO : request.getScrapQty());
        report.setWorkHours(request.getWorkHours());
        report.setRemark(request.getRemark());
        this.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        PmReportWork exist = this.getById(id);
        if (exist == null) {
            throw new BizException("报工单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmReportWork.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的报工单才能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReport(Long id) {
        PmReportWork report = this.getById(id);
        if (report == null) {
            throw new BizException("报工单不存在");
        }
        if (report.getStatus() == null || report.getStatus() != PmReportWork.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的报工单才能审核");
        }
        PmMo mo = moMapper.selectById(report.getMoId());
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }

        PmRoutingOperation operation = report.getOperationId() == null
                ? null : operationMapper.selectById(report.getOperationId());
        BigDecimal laborRate = operation == null || operation.getLaborRate() == null
                ? BigDecimal.ZERO : operation.getLaborRate();
        BigDecimal overheadRate = operation == null || operation.getOverheadRate() == null
                ? BigDecimal.ZERO : operation.getOverheadRate();

        // 归集人工成本与制造费用
        costService.collectReportCost(report, laborRate, overheadRate);

        BigDecimal workHours = report.getWorkHours() == null ? BigDecimal.ZERO : report.getWorkHours();
        BigDecimal laborAmount = workHours.multiply(laborRate).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal overheadAmount = workHours.multiply(overheadRate).setScale(SCALE, RoundingMode.HALF_UP);

        // 凭证：借 生产成本（归集人工与制造费用），贷 应付职工薪酬
        // 制造费用部分与人工部分分开记账，便于成本分析
        Long voucherId = null;
        if (laborAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<VoucherLine> lines = new ArrayList<>();
            lines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("COST")
                    .amount(laborAmount)
                    .summary("生产报工人工成本 " + report.getReportNo())
                    .build());
            lines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("PAYABLE")
                    .amount(laborAmount)
                    .summary("生产报工应付职工薪酬 " + report.getReportNo())
                    .build());
            voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, report.getReportDate(),
                    "生产报工 " + report.getReportNo(), lines);
        }
        if (overheadAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<VoucherLine> lines = new ArrayList<>();
            lines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_DEBIT)
                    .ruleKey("COST")
                    .amount(overheadAmount)
                    .summary("生产报工制造费用 " + report.getReportNo())
                    .build());
            lines.add(VoucherLine.builder()
                    .direction(com.erp.fi.domain.entity.FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("OVERHEAD")
                    .amount(overheadAmount)
                    .summary("生产报工制造费用结转 " + report.getReportNo())
                    .build());
            Long overheadVoucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, report.getReportDate(),
                    "生产报工(制造费用) " + report.getReportNo(), lines);
            if (voucherId == null) {
                voucherId = overheadVoucherId;
            }
        }

        PmReportWork update = new PmReportWork();
        update.setId(id);
        update.setStatus(PmReportWork.STATUS_AUDITED);
        update.setVoucherId(voucherId);
        this.updateById(update);

        // 回写生产订单累计完工量
        moMapper.addProducedQty(report.getMoId(), report.getQualifiedQty());
        PmMo produced = moMapper.selectById(report.getMoId());
        if (produced != null && produced.getStatus() != null
                && produced.getStatus() == PmMo.STATUS_AUDITED) {
            // 报工说明已经开工，工单转入生产中
            PmMo moUpdate = new PmMo();
            moUpdate.setId(produced.getId());
            moUpdate.setStatus(PmMo.STATUS_PROCESSING);
            moMapper.updateById(moUpdate);
        }
        costService.refreshMoCost(produced);
    }

    /**
     * 合格数量：未填写时按报工数量计（无报废）。
     */
    private BigDecimal resolveQualifiedQty(PmReportWorkSaveRequest request) {
        if (request.getQualifiedQty() != null) {
            return request.getQualifiedQty();
        }
        BigDecimal scrap = request.getScrapQty() == null ? BigDecimal.ZERO : request.getScrapQty();
        BigDecimal qualified = request.getQty().subtract(scrap);
        return qualified.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : qualified;
    }

    /**
     * 校验数量关系：合格数 + 报废数 = 报工数量，且合格数不得超过工单剩余产量。
     */
    private void validateQty(PmReportWorkSaveRequest request) {
        BigDecimal scrap = request.getScrapQty() == null ? BigDecimal.ZERO : request.getScrapQty();
        BigDecimal qualified = resolveQualifiedQty(request);
        if (qualified.add(scrap).compareTo(request.getQty()) != 0) {
            throw new BizException("合格数量与报废数量之和必须等于报工数量");
        }
    }

    private void validateOperation(Long operationId) {
        if (operationId == null) {
            return;
        }
        PmRoutingOperation operation = operationMapper.selectById(operationId);
        if (operation == null) {
            throw new BizException("工序不存在");
        }
    }

    private PmReportWorkVO toVO(PmReportWork report) {
        PmReportWorkVO vo = new PmReportWorkVO();
        BeanUtils.copyProperties(report, vo);
        PmMo mo = moMapper.selectById(report.getMoId());
        if (mo != null) {
            vo.setMoNo(mo.getMoNo());
            MdMaterial material = materialMapper.selectById(mo.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getMaterialName());
            }
        }
        if (report.getOperationId() != null) {
            PmRoutingOperation operation = operationMapper.selectById(report.getOperationId());
            if (operation != null) {
                vo.setOperationNo(operation.getOperationNo());
                vo.setOperationName(operation.getOperationName());
            }
        }
        if (report.getEmployeeId() != null) {
            HrEmployee employee = employeeMapper.selectById(report.getEmployeeId());
            if (employee != null) {
                vo.setEmployeeName(employee.getEmployeeName());
            }
        }
        // 人工/制造费用从归集记录反查
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        for (com.erp.pm.domain.entity.PmManufacturingCost cost : costService
                .listReportCosts(report.getMoId(), report.getId())) {
            labor = labor.add(cost.getLaborAmount() == null ? BigDecimal.ZERO : cost.getLaborAmount());
            overhead = overhead.add(cost.getOverheadAmount() == null ? BigDecimal.ZERO : cost.getOverheadAmount());
        }
        vo.setLaborCost(labor);
        vo.setOverheadCost(overhead);
        return vo;
    }

    private String nextNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("RW", date.format(MONTH_FORMATTER));
    }

}








