package com.erp.hr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.service.FiVoucherService;
import com.erp.hr.domain.dto.HrSalaryEntryRequest;
import com.erp.hr.domain.dto.HrSalaryQueryRequest;
import com.erp.hr.domain.dto.HrSalarySaveRequest;
import com.erp.hr.domain.entity.HrDept;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.domain.entity.HrSalary;
import com.erp.hr.domain.entity.HrSalaryEntry;
import com.erp.hr.domain.vo.HrSalaryEntryVO;
import com.erp.hr.domain.vo.HrSalaryVO;
import com.erp.hr.mapper.HrDeptMapper;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.hr.mapper.HrSalaryEntryMapper;
import com.erp.hr.mapper.HrSalaryMapper;
import com.erp.hr.service.HrSalaryService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrSalaryServiceImpl extends ServiceImpl<HrSalaryMapper, HrSalary> implements HrSalaryService {

    private static final String NO_PREFIX = "GZ-";
    private static final String VOUCHER_BIZ_TYPE = "HR_SALARY";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final HrSalaryEntryMapper salaryEntryMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrDeptMapper deptMapper;
    private final FiVoucherService voucherService;

    @Override
    public PageResult<HrSalaryVO> pageSalary(HrSalaryQueryRequest request) {
        Page<HrSalary> page = this.page(request.toPage(), Wrappers.<HrSalary>lambdaQuery()
                .eq(StringUtils.hasLength(request.getPeriod()), HrSalary::getPeriod, request.getPeriod())
                .eq(request.getEmployeeId() != null, HrSalary::getEmployeeId, request.getEmployeeId())
                .eq(request.getStatus() != null, HrSalary::getStatus, request.getStatus())
                .orderByDesc(HrSalary::getId));
        List<HrSalaryVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<HrSalaryVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public HrSalaryVO getSalaryDetail(Long id) {
        HrSalary salary = this.getById(id);
        if (salary == null) {
            throw new BizException("薪资单不存在");
        }
        HrSalaryVO vo = toVO(salary);
        vo.setEntries(loadEntries(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalary(HrSalarySaveRequest request) {
        HrEmployee employee = employeeMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        Long count = this.count(Wrappers.<HrSalary>lambdaQuery()
                .eq(HrSalary::getPeriod, request.getPeriod())
                .eq(HrSalary::getEmployeeId, request.getEmployeeId()));
        if (count != null && count > 0) {
            throw new BizException("该员工本期间薪资单已存在");
        }
        SalaryDraft draft = buildDraft(request);
        HrSalary salary = new HrSalary();
        salary.setSalaryNo(nextNo(request.getPeriod()));
        salary.setPeriod(request.getPeriod());
        salary.setEmployeeId(request.getEmployeeId());
        salary.setDeptId(employee.getDeptId());
        salary.setBaseAmount(draft.getBase());
        salary.setMeritAmount(draft.getMerit());
        salary.setAllowanceAmount(draft.getAllowance());
        salary.setDeductAmount(draft.getDeduct());
        salary.setSocialAmount(draft.getSocial());
        salary.setNetAmount(draft.getNet());
        salary.setGrossAmount(draft.getGross());
        salary.setStatus(HrSalary.STATUS_DRAFT);
        salary.setRemark(request.getRemark());
        this.save(salary);
        saveEntries(salary.getId(), request.getEntries());
        return salary.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalary(HrSalarySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        HrSalary exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("薪资单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != HrSalary.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的薪资单才能修改");
        }
        HrEmployee employee = employeeMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        SalaryDraft draft = buildDraft(request);
        HrSalary salary = new HrSalary();
        salary.setId(request.getId());
        salary.setPeriod(request.getPeriod());
        salary.setEmployeeId(request.getEmployeeId());
        salary.setDeptId(employee.getDeptId());
        salary.setBaseAmount(draft.getBase());
        salary.setMeritAmount(draft.getMerit());
        salary.setAllowanceAmount(draft.getAllowance());
        salary.setDeductAmount(draft.getDeduct());
        salary.setSocialAmount(draft.getSocial());
        salary.setNetAmount(draft.getNet());
        salary.setGrossAmount(draft.getGross());
        salary.setRemark(request.getRemark());
        this.updateById(salary);
        salaryEntryMapper.delete(Wrappers.<HrSalaryEntry>lambdaQuery().eq(HrSalaryEntry::getSalaryId, request.getId()));
        saveEntries(request.getId(), request.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalary(Long id) {
        HrSalary exist = this.getById(id);
        if (exist == null) {
            throw new BizException("薪资单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != HrSalary.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的薪资单才能删除");
        }
        this.removeById(id);
        salaryEntryMapper.delete(Wrappers.<HrSalaryEntry>lambdaQuery().eq(HrSalaryEntry::getSalaryId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSalary(Long id) {
        HrSalary salary = this.getById(id);
        if (salary == null) {
            throw new BizException("薪资单不存在");
        }
        if (salary.getStatus() == null || salary.getStatus() != HrSalary.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的薪资单才能审核");
        }
        if (salary.getNetAmount() == null || salary.getNetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("实发金额必须大于0");
        }

        BigDecimal gross = salary.getGrossAmount() == null
                ? salary.getBaseAmount().add(salary.getMeritAmount()).add(salary.getAllowanceAmount())
                : salary.getGrossAmount();
        BigDecimal socialPersonal = salary.getSocialPersonal() == null
                ? BigDecimal.ZERO : salary.getSocialPersonal();
        BigDecimal socialCompany = salary.getSocialCompany() == null
                ? BigDecimal.ZERO : salary.getSocialCompany();
        BigDecimal taxAmount = salary.getTaxAmount() == null
                ? BigDecimal.ZERO : salary.getTaxAmount();

        // 职工薪酬的账面成本 = 应发工资 + 单位承担的社保公积金
        BigDecimal totalCost = gross.add(socialCompany);

        List<VoucherLine> lines = new ArrayList<>();
        lines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_DEBIT)
                .ruleKey("EXPENSE")
                .amount(totalCost)
                .summary("计提职工薪酬")
                .build());
        lines.add(VoucherLine.builder()
                .direction(FiVoucherEntry.DIRECTION_CREDIT)
                .ruleKey("PAYABLE")
                .amount(gross)
                .summary("应付职工薪酬-工资")
                .build());
        if (socialPersonal.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("SOCIAL")
                    .amount(socialPersonal)
                    .summary("应付职工薪酬-社会保险费")
                    .build());
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(VoucherLine.builder()
                    .direction(FiVoucherEntry.DIRECTION_CREDIT)
                    .ruleKey("TAX")
                    .amount(taxAmount)
                    .summary("代扣个人所得税")
                    .build());
        }

        LocalDate voucherDate = YearMonth.parse(salary.getPeriod()).atEndOfMonth();
        Long voucherId = voucherService.generateBizVoucher(VOUCHER_BIZ_TYPE, voucherDate,
                "薪资计提 " + salary.getSalaryNo(), lines);
        HrSalary update = new HrSalary();
        update.setId(id);
        update.setStatus(HrSalary.STATUS_AUDITED);
        update.setVoucherId(voucherId);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
    }

    private SalaryDraft buildDraft(HrSalarySaveRequest request) {
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal merit = BigDecimal.ZERO;
        BigDecimal allowance = BigDecimal.ZERO;
        BigDecimal deduct = BigDecimal.ZERO;
        BigDecimal social = BigDecimal.ZERO;
        for (HrSalaryEntryRequest entryRequest : request.getEntries()) {
            if (entryRequest.getItemType() == null) {
                throw new BizException("薪资项目类型不能为空");
            }
            switch (entryRequest.getItemType()) {
                case HrSalary.ITEM_BASE:
                    base = base.add(entryRequest.getAmount());
                    break;
                case HrSalary.ITEM_MERIT:
                    merit = merit.add(entryRequest.getAmount());
                    break;
                case HrSalary.ITEM_ALLOWANCE:
                    allowance = allowance.add(entryRequest.getAmount());
                    break;
                case HrSalary.ITEM_DEDUCT:
                    deduct = deduct.add(entryRequest.getAmount());
                    break;
                case HrSalary.ITEM_SOCIAL:
                    social = social.add(entryRequest.getAmount());
                    break;
                default:
                    throw new BizException("薪资项目类型不合法");
            }
        }
        BigDecimal gross = base.add(merit).add(allowance);
        BigDecimal net = gross.subtract(deduct).subtract(social);
        if (net.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("实发金额必须大于0");
        }
        SalaryDraft draft = new SalaryDraft();
        draft.setBase(base);
        draft.setMerit(merit);
        draft.setAllowance(allowance);
        draft.setDeduct(deduct);
        draft.setSocial(social);
        draft.setNet(net);
        draft.setGross(gross);
        return draft;
    }

    private void saveEntries(Long salaryId, List<HrSalaryEntryRequest> entryRequests) {
        for (HrSalaryEntryRequest entryRequest : entryRequests) {
            HrSalaryEntry entry = new HrSalaryEntry();
            entry.setSalaryId(salaryId);
            entry.setItemType(entryRequest.getItemType());
            entry.setItemName(entryRequest.getItemName());
            entry.setAmount(entryRequest.getAmount());
            salaryEntryMapper.insert(entry);
        }
    }

    private List<HrSalaryEntryVO> loadEntries(Long salaryId) {
        List<HrSalaryEntry> entries = salaryEntryMapper.selectList(Wrappers.<HrSalaryEntry>lambdaQuery()
                .eq(HrSalaryEntry::getSalaryId, salaryId)
                .orderByAsc(HrSalaryEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.stream().map(entry -> {
            HrSalaryEntryVO vo = new HrSalaryEntryVO();
            BeanUtils.copyProperties(entry, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private HrSalaryVO toVO(HrSalary salary) {
        HrSalaryVO vo = new HrSalaryVO();
        BeanUtils.copyProperties(salary, vo);
        HrEmployee employee = employeeMapper.selectById(salary.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeNo(employee.getEmployeeNo());
            vo.setEmployeeName(employee.getEmployeeName());
        }
        if (salary.getDeptId() != null) {
            HrDept dept = deptMapper.selectById(salary.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }

    private String nextNo(String period) {
        String prefix = NO_PREFIX + period.replace("-", "") + "-";
        return prefix + nextSeq(baseMapper.selectLastSalaryNo(prefix));
    }

    private String nextSeq(String lastNo) {
        int next = 1;
        if (lastNo != null) {
            int idx = lastNo.lastIndexOf('-');
            if (idx >= 0 && idx < lastNo.length() - 1) {
                try {
                    next = Integer.parseInt(lastNo.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return String.format("%04d", next);
    }

    private static class SalaryDraft {
        private BigDecimal base;
        private BigDecimal merit;
        private BigDecimal allowance;
        private BigDecimal deduct;
        private BigDecimal social;
        private BigDecimal net;
        private BigDecimal gross;

        private BigDecimal getBase() {
            return base;
        }

        private void setBase(BigDecimal base) {
            this.base = base;
        }

        private BigDecimal getMerit() {
            return merit;
        }

        private void setMerit(BigDecimal merit) {
            this.merit = merit;
        }

        private BigDecimal getAllowance() {
            return allowance;
        }

        private void setAllowance(BigDecimal allowance) {
            this.allowance = allowance;
        }

        private BigDecimal getDeduct() {
            return deduct;
        }

        private void setDeduct(BigDecimal deduct) {
            this.deduct = deduct;
        }

        private BigDecimal getSocial() {
            return social;
        }

        private void setSocial(BigDecimal social) {
            this.social = social;
        }

        private BigDecimal getNet() {
            return net;
        }

        private BigDecimal getGross() {
            return gross;
        }

        private void setGross(BigDecimal gross) {
            this.gross = gross;
        }

        private void setNet(BigDecimal net) {
            this.net = net;
        }
    }
}








