package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.VoucherEntryRequest;
import com.erp.fi.domain.dto.VoucherLine;
import com.erp.fi.domain.dto.VoucherQueryRequest;
import com.erp.fi.domain.dto.VoucherSaveRequest;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.entity.FiBizAccountRule;
import com.erp.fi.domain.entity.FiPeriod;
import com.erp.fi.domain.entity.FiVoucher;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.domain.vo.VoucherDetailVO;
import com.erp.fi.domain.vo.VoucherEntryVO;
import com.erp.fi.domain.vo.VoucherVO;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiBizAccountRuleMapper;
import com.erp.fi.mapper.FiPeriodMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import com.erp.fi.mapper.FiVoucherMapper;
import com.erp.fi.service.FiVoucherService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiVoucherServiceImpl extends ServiceImpl<FiVoucherMapper, FiVoucher> implements FiVoucherService {

    /** 凭证号重试次数，覆盖并发取号撞唯一键的场景 */
    private static final int VOUCHER_NO_MAX_RETRY = 5;

    private final FiVoucherEntryMapper voucherEntryMapper;
    private final FiAccountMapper accountMapper;
    private final FiPeriodMapper periodMapper;
    private final FiBizAccountRuleMapper ruleMapper;

    @Override
    public PageResult<VoucherVO> pageVoucher(VoucherQueryRequest request) {
        Page<FiVoucher> page = this.page(request.toPage(), Wrappers.<FiVoucher>lambdaQuery()
                .like(StringUtils.hasText(request.getVoucherNo()), FiVoucher::getVoucherNo, request.getVoucherNo())
                .eq(StringUtils.hasText(request.getPeriod()), FiVoucher::getPeriod, request.getPeriod())
                .eq(request.getStatus() != null, FiVoucher::getStatus, request.getStatus())
                .ge(request.getStartDate() != null, FiVoucher::getVoucherDate, request.getStartDate())
                .le(request.getEndDate() != null, FiVoucher::getVoucherDate, request.getEndDate())
                .orderByDesc(FiVoucher::getVoucherDate)
                .orderByDesc(FiVoucher::getId));
        List<VoucherVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<VoucherVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public VoucherDetailVO getVoucherDetail(Long id) {
        FiVoucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        VoucherDetailVO detail = new VoucherDetailVO();
        BeanUtils.copyProperties(voucher, detail);
        detail.setEntries(loadEntryVOs(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucher(VoucherSaveRequest request) {
        String period = resolvePeriod(request.getVoucherDate());
        VoucherDraft draft = buildDraft(request, period);
        FiVoucher voucher = draft.getVoucher();
        voucher.setStatus(FiVoucher.STATUS_DRAFT);
        voucher.setRedFlush(0);
        return saveVoucherWithRetry(voucher, draft.getEntries());
    }

    /**
     * 保存凭证，凭证号冲突时重新取号重试。
     * <p>
     * 并发场景下多个请求可能算出同一个凭证号，唯一键会拒绝其中一个，
     * 此时重新读取最大序号即可得到新号。MySQL 的重复键错误不会中止事务，
     * 因此可以在同一事务内安全重试。
     */
    private Long saveVoucherWithRetry(FiVoucher voucher, List<FiVoucherEntry> entries) {
        for (int attempt = 0; attempt < VOUCHER_NO_MAX_RETRY; attempt++) {
            voucher.setVoucherNo(nextVoucherNo(voucher.getPeriod()));
            try {
                this.save(voucher);
                saveEntries(voucher.getId(), entries);
                return voucher.getId();
            } catch (DuplicateKeyException e) {
                if (attempt == VOUCHER_NO_MAX_RETRY - 1) {
                    throw new BizException("凭证号生成冲突，请稍后重试");
                }
            }
        }
        throw new BizException("凭证号生成冲突，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucher(VoucherSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        FiVoucher exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(exist.getStatus(), FiVoucher.STATUS_DRAFT)) {
            throw new BizException("只有暂存状态的凭证才能修改");
        }
        String period = resolvePeriod(request.getVoucherDate());
        VoucherDraft draft = buildDraft(request, period);
        FiVoucher voucher = draft.getVoucher();
        voucher.setId(request.getId());
        this.updateById(voucher);
        voucherEntryMapper.delete(Wrappers.<FiVoucherEntry>lambdaQuery().eq(FiVoucherEntry::getVoucherId, request.getId()));
        saveEntries(request.getId(), draft.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVoucher(Long id) {
        FiVoucher exist = this.getById(id);
        if (exist == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(exist.getStatus(), FiVoucher.STATUS_DRAFT)) {
            throw new BizException("只有暂存状态的凭证才能删除");
        }
        this.removeById(id);
        voucherEntryMapper.delete(Wrappers.<FiVoucherEntry>lambdaQuery().eq(FiVoucherEntry::getVoucherId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitVoucher(Long id) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(voucher.getStatus(), FiVoucher.STATUS_DRAFT)) {
            throw new BizException("只有暂存状态的凭证才能提交");
        }
        checkPeriodOpen(voucher.getPeriod());
        checkBalanced(voucher.getId());
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_SUBMITTED);
        update.setSubmitBy(userId);
        update.setSubmitTime(now);
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVoucher(Long id) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(voucher.getStatus(), FiVoucher.STATUS_SUBMITTED)) {
            throw new BizException("只有已提交状态的凭证才能审核");
        }
        checkSeparationOfDuties(voucher);
        Long userId = SecurityUtils.getCurrentUserId();
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_AUDITED);
        update.setAuditBy(userId);
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
    }

    /**
     * 反审核：把已审核凭证退回已提交状态。
     * <p>
     * 审核人才能反审核，且期间必须仍开放——期间一旦结账，凭证内容即被冻结。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditVoucher(Long id) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(voucher.getStatus(), FiVoucher.STATUS_AUDITED)) {
            throw new BizException("只有已审核状态的凭证才能反审核");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (voucher.getAuditBy() != null && voucher.getAuditBy().equals(userId)) {
            throw new BizException("不能反审核自己审核的凭证");
        }
        checkPeriodOpen(voucher.getPeriod());
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_SUBMITTED);
        update.setAuditBy(null);
        update.setAuditTime(null);
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postVoucher(Long id) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(voucher.getStatus(), FiVoucher.STATUS_AUDITED)) {
            throw new BizException("只有已审核状态的凭证才能记账");
        }
        checkPeriodOpen(voucher.getPeriod());
        checkBalanced(voucher.getId());
        Long userId = SecurityUtils.getCurrentUserId();
        // 业务单据自动生成的凭证由业务操作触发记账，不适用「记账人≠审核人」约束
        boolean autoGenerated = voucher.getSourceType() != null && !voucher.getSourceType().isBlank();
        if (!autoGenerated && voucher.getAuditBy() != null && voucher.getAuditBy().equals(userId)) {
            throw new BizException("不能记账自己审核的凭证");
        }
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_POSTED);
        update.setPostBy(userId);
        update.setPostTime(LocalDateTime.now());
        this.updateById(update);
    }

    /**
     * 反记账：撤销已记账凭证，使其退出账簿。
     * <p>
     * 仅允许在期间未结账时执行，避免历史期间数据被动摇。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpostVoucher(Long id) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(voucher.getStatus(), FiVoucher.STATUS_POSTED)) {
            throw new BizException("只有已记账状态的凭证才能反记账");
        }
        checkPeriodOpen(voucher.getPeriod());
        Long userId = SecurityUtils.getCurrentUserId();
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_AUDITED);
        update.setPostBy(null);
        update.setPostTime(null);
        update.setUnpostBy(userId);
        update.setUnpostTime(LocalDateTime.now());
        this.updateById(update);
    }

    /**
     * 作废凭证。作废后凭证不再参与任何账簿计算，但保留痕迹以备审计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidVoucher(Long id, String reason) {
        FiVoucher voucher = getById(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        if (statusIs(voucher.getStatus(), FiVoucher.STATUS_POSTED)) {
            throw new BizException("已记账的凭证不能作废，请先反记账");
        }
        if (statusIs(voucher.getStatus(), FiVoucher.STATUS_VOID)) {
            throw new BizException("该凭证已作废");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BizException("作废原因不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        FiVoucher update = new FiVoucher();
        update.setId(id);
        update.setStatus(FiVoucher.STATUS_VOID);
        update.setVoidReason(reason);
        update.setVoidBy(userId);
        update.setVoidTime(LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long redFlushVoucher(Long id) {
        FiVoucher original = getById(id);
        if (original == null) {
            throw new BizException("凭证不存在");
        }
        if (!statusIs(original.getStatus(), FiVoucher.STATUS_POSTED)) {
            throw new BizException("只有已记账状态的凭证才能红字冲销");
        }
        checkPeriodOpen(original.getPeriod());
        List<FiVoucherEntry> originalEntries = voucherEntryMapper.selectList(Wrappers.<FiVoucherEntry>lambdaQuery()
                .eq(FiVoucherEntry::getVoucherId, id)
                .orderByAsc(FiVoucherEntry::getSortNo));
        if (originalEntries.isEmpty()) {
            throw new BizException("原凭证明细为空，无法冲销");
        }
        FiVoucher red = new FiVoucher();
        red.setWord(original.getWord());
        red.setVoucherDate(original.getVoucherDate());
        red.setPeriod(original.getPeriod());
        red.setAttachCount(original.getAttachCount());
        red.setSummary("红冲：" + (original.getSummary() == null ? original.getVoucherNo() : original.getSummary()));
        red.setRedFlush(1);
        red.setSourceVoucherId(original.getId());
        red.setStatus(FiVoucher.STATUS_DRAFT);
        red.setSourceType(original.getSourceType());
        red.setSourceId(original.getSourceId());
        List<FiVoucherEntry> redEntries = new ArrayList<>(originalEntries.size());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (FiVoucherEntry entry : originalEntries) {
            FiVoucherEntry redEntry = new FiVoucherEntry();
            redEntry.setAccountId(entry.getAccountId());
            redEntry.setDirection(entry.getDirection());
            redEntry.setAmount(entry.getAmount().negate());
            redEntry.setSummary(entry.getSummary());
            redEntry.setAssist(entry.getAssist());
            redEntry.setAuxType(entry.getAuxType());
            redEntry.setAuxId(entry.getAuxId());
            redEntry.setCurrency(entry.getCurrency());
            redEntry.setOriginalAmount(entry.getOriginalAmount() == null ? null : entry.getOriginalAmount().negate());
            redEntry.setExchangeRate(entry.getExchangeRate());
            redEntry.setSortNo(entry.getSortNo());
            redEntries.add(redEntry);
            if (directionIs(entry.getDirection(), FiVoucherEntry.DIRECTION_DEBIT)) {
                totalDebit = totalDebit.add(redEntry.getAmount());
            } else {
                totalCredit = totalCredit.add(redEntry.getAmount());
            }
        }
        red.setTotalDebit(totalDebit);
        red.setTotalCredit(totalCredit);
        return saveVoucherWithRetry(red, redEntries);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateBizVoucher(String bizType, LocalDate voucherDate, String summary, List<VoucherLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BizException("凭证分录不能为空");
        }
        String period = resolvePeriod(voucherDate);
        checkPeriodOpen(period);
        Map<String, FiBizAccountRule> ruleMap = ruleMapper.selectList(Wrappers.<FiBizAccountRule>lambdaQuery()
                        .eq(FiBizAccountRule::getBizType, bizType))
                .stream()
                .collect(Collectors.toMap(FiBizAccountRule::getRuleKey, rule -> rule, (a, b) -> a));
        List<String> subjectCodes = new ArrayList<>();
        ruleMap.values().forEach(rule -> subjectCodes.add(rule.getSubjectCode()));
        lines.forEach(line -> {
            if (StringUtils.hasText(line.getSubjectCode())) {
                subjectCodes.add(line.getSubjectCode());
            }
        });
        List<String> distinctCodes = subjectCodes.stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        Map<String, FiAccount> accountMap = distinctCodes.isEmpty()
                ? Map.of()
                : accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                        .in(FiAccount::getSubjectCode, distinctCodes))
                .stream()
                .collect(Collectors.toMap(FiAccount::getSubjectCode, account -> account, (a, b) -> a));

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<FiVoucherEntry> entries = new ArrayList<>(lines.size());
        int sortNo = 0;
        for (VoucherLine line : lines) {
            String subjectCode = line.getSubjectCode();
            if (!StringUtils.hasText(subjectCode)) {
                FiBizAccountRule rule = ruleMap.get(line.getRuleKey());
                if (rule == null) {
                    throw new BizException("凭证科目规则未配置：" + bizType + "/" + line.getRuleKey());
                }
                subjectCode = rule.getSubjectCode();
            }
            FiAccount account = accountMap.get(subjectCode);
            if (account == null) {
                throw new BizException("科目不存在：" + subjectCode);
            }
            if (account.getStatus() != null && account.getStatus() != 0) {
                throw new BizException("会计科目已停用：" + account.getSubjectCode());
            }
            if (!isLeafAccount(account)) {
                throw new BizException("非明细科目不能记账：" + account.getSubjectCode());
            }
            BigDecimal amount = line.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new BizException("分录金额不能为零");
            }
            if (!directionIs(line.getDirection(), FiVoucherEntry.DIRECTION_DEBIT)
                    && !directionIs(line.getDirection(), FiVoucherEntry.DIRECTION_CREDIT)) {
                throw new BizException("借贷方向不合法");
            }
            sortNo++;
            FiVoucherEntry entry = new FiVoucherEntry();
            entry.setAccountId(account.getId());
            entry.setDirection(line.getDirection());
            entry.setAmount(amount);
            entry.setSummary(line.getSummary());
            entry.setAuxType(line.getAuxType());
            entry.setAuxId(line.getAuxId());
            if (line.getAuxType() != null && line.getAuxId() != null) {
                entry.setAssist(line.getAuxName());
            }
            entry.setCurrency(line.getCurrency());
            entry.setOriginalAmount(line.getOriginalAmount());
            entry.setExchangeRate(line.getExchangeRate());
            entry.setSortNo(sortNo);
            entries.add(entry);
            if (directionIs(line.getDirection(), FiVoucherEntry.DIRECTION_DEBIT)) {
                totalDebit = totalDebit.add(amount);
            } else {
                totalCredit = totalCredit.add(amount);
            }
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BizException("凭证借贷不平，无法生成");
        }
        FiVoucher voucher = new FiVoucher();
        voucher.setVoucherNo(nextVoucherNo(period));
        voucher.setWord("记");
        voucher.setVoucherDate(voucherDate);
        voucher.setPeriod(period);
        voucher.setAttachCount(0);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setStatus(FiVoucher.STATUS_SUBMITTED);
        voucher.setRedFlush(0);
        voucher.setSummary(summary);
        voucher.setSourceType(bizType);
        voucher.setSubmitBy(SecurityUtils.getCurrentUserId());
        voucher.setSubmitTime(LocalDateTime.now());
        // 业务单据审核即代表业务已发生，凭证同步进入账簿。
        // 手工录入的凭证仍走「提交→审核→记账」三步，业务与技术凭证的记账节奏本来就不一致。
        voucher.setStatus(FiVoucher.STATUS_POSTED);
        voucher.setAuditBy(SecurityUtils.getCurrentUserId());
        voucher.setAuditTime(LocalDateTime.now());
        voucher.setPostBy(SecurityUtils.getCurrentUserId());
        voucher.setPostTime(LocalDateTime.now());
        return saveVoucherWithRetry(voucher, entries);
    }

    private static boolean statusIs(Integer status, int expected) {
        return status != null && status == expected;
    }

    private static boolean directionIs(Integer direction, int expected) {
        return direction != null && direction == expected;
    }

    /**
     * 判断是否为明细科目。优先使用 is_leaf 标记，缺失时回退到子科目数量判断。
     */
    private boolean isLeafAccount(FiAccount account) {
        if (account.getIsLeaf() != null) {
            return account.getIsLeaf() == 1;
        }
        Long childCount = accountMapper.selectCount(Wrappers.<FiAccount>lambdaQuery()
                .eq(FiAccount::getParentId, account.getId()));
        return childCount == null || childCount == 0;
    }

    /**
     * 制单人与审核人职责分离校验。
     * <p>
     * 内控红线：同一人不得既制单又审核，否则审核环节形同虚设。
     * <p>
     * 例外：业务单据自动生成的凭证（sourceType 非空）不适用该校验——
     * 这类凭证由业务操作触发而非财务人员手工编制，再要求另一人审核会让
     * 业务单据无法闭环。其内控由业务单据自身的审批流保证。
     */
    private void checkSeparationOfDuties(FiVoucher voucher) {
        if (voucher.getSourceType() != null && !voucher.getSourceType().isBlank()) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }
        if (voucher.getSubmitBy() != null && voucher.getSubmitBy().equals(userId)) {
            throw new BizException("制单人不能审核自己提交的凭证");
        }
    }

    private VoucherDraft buildDraft(VoucherSaveRequest request, String period) {
        checkPeriodOpen(period);
        List<VoucherEntryRequest> entryRequests = request.getEntries();
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("凭证明细不能为空");
        }
        Map<Long, FiAccount> accountMap = loadAccounts(entryRequests);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<FiVoucherEntry> entries = new ArrayList<>(entryRequests.size());
        for (int i = 0; i < entryRequests.size(); i++) {
            VoucherEntryRequest entryRequest = entryRequests.get(i);
            FiAccount account = accountMap.get(entryRequest.getAccountId());
            if (account == null) {
                throw new BizException("会计科目不存在：" + entryRequest.getAccountId());
            }
            if (account.getStatus() != null && account.getStatus() != 0) {
                throw new BizException("会计科目已停用：" + account.getSubjectCode());
            }
            if (!isLeafAccount(account)) {
                throw new BizException("非明细科目不能记账：" + account.getSubjectCode());
            }
            BigDecimal amount = entryRequest.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("分录金额必须大于0");
            }
            if (!directionIs(entryRequest.getDirection(), FiVoucherEntry.DIRECTION_DEBIT)
                    && !directionIs(entryRequest.getDirection(), FiVoucherEntry.DIRECTION_CREDIT)) {
                throw new BizException("借贷方向不合法");
            }
            FiVoucherEntry entry = new FiVoucherEntry();
            entry.setAccountId(account.getId());
            entry.setDirection(entryRequest.getDirection());
            entry.setAmount(amount);
            entry.setSummary(entryRequest.getSummary());
            entry.setAssist(entryRequest.getAssist());
            entry.setAuxType(entryRequest.getAuxType());
            entry.setAuxId(entryRequest.getAuxId());
            entry.setSortNo(i + 1);
            entries.add(entry);
            if (FiVoucherEntry.DIRECTION_DEBIT == entryRequest.getDirection()) {
                totalDebit = totalDebit.add(amount);
            } else {
                totalCredit = totalCredit.add(amount);
            }
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BizException("凭证借贷不平，无法保存");
        }
        FiVoucher voucher = new FiVoucher();
        voucher.setWord(StringUtils.hasText(request.getWord()) ? request.getWord() : "记");
        voucher.setVoucherDate(request.getVoucherDate());
        voucher.setPeriod(period);
        voucher.setAttachCount(request.getAttachCount() == null ? 0 : request.getAttachCount());
        voucher.setSummary(request.getSummary());
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        return new VoucherDraft(voucher, entries);
    }

    private Map<Long, FiAccount> loadAccounts(List<VoucherEntryRequest> entryRequests) {
        List<Long> accountIds = entryRequests.stream()
                .map(VoucherEntryRequest::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            throw new BizException("凭证明细不能为空");
        }
        List<FiAccount> accounts = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                .in(FiAccount::getId, accountIds));
        return accounts.stream().collect(Collectors.toMap(FiAccount::getId, Function.identity()));
    }

    private void saveEntries(Long voucherId, List<FiVoucherEntry> entries) {
        for (FiVoucherEntry entry : entries) {
            entry.setId(null);
            entry.setVoucherId(voucherId);
            voucherEntryMapper.insert(entry);
        }
    }

    private List<VoucherEntryVO> loadEntryVOs(Long voucherId) {
        List<FiVoucherEntry> entries = voucherEntryMapper.selectList(Wrappers.<FiVoucherEntry>lambdaQuery()
                .eq(FiVoucherEntry::getVoucherId, voucherId)
                .orderByAsc(FiVoucherEntry::getSortNo));
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> accountIds = entries.stream().map(FiVoucherEntry::getAccountId).distinct().collect(Collectors.toList());
        Map<Long, FiAccount> accountMap = accountMapper.selectList(Wrappers.<FiAccount>lambdaQuery()
                        .in(FiAccount::getId, accountIds))
                .stream()
                .collect(Collectors.toMap(FiAccount::getId, Function.identity()));
        List<VoucherEntryVO> vos = new ArrayList<>(entries.size());
        for (FiVoucherEntry entry : entries) {
            VoucherEntryVO vo = new VoucherEntryVO();
            BeanUtils.copyProperties(entry, vo);
            FiAccount account = accountMap.get(entry.getAccountId());
            if (account != null) {
                vo.setAccountCode(account.getSubjectCode());
                vo.setAccountName(account.getSubjectName());
            }
            vos.add(vo);
        }
        return vos;
    }

    private void checkBalanced(Long voucherId) {
        List<FiVoucherEntry> entries = voucherEntryMapper.selectList(Wrappers.<FiVoucherEntry>lambdaQuery()
                .eq(FiVoucherEntry::getVoucherId, voucherId));
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (FiVoucherEntry entry : entries) {
            if (FiVoucherEntry.DIRECTION_DEBIT == entry.getDirection()) {
                totalDebit = totalDebit.add(entry.getAmount());
            } else if (FiVoucherEntry.DIRECTION_CREDIT == entry.getDirection()) {
                totalCredit = totalCredit.add(entry.getAmount());
            }
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BizException("凭证借贷不平，无法提交");
        }
    }

    private void checkPeriodOpen(String period) {
        FiPeriod fiPeriod = periodMapper.selectOne(Wrappers.<FiPeriod>lambdaQuery()
                .eq(FiPeriod::getPeriodMonth, period)
                .last("limit 1"));
        if (fiPeriod == null) {
            throw new BizException("会计期间不存在：" + period);
        }
        if (fiPeriod.getStatus() == null || fiPeriod.getStatus() != FiPeriod.STATUS_OPEN) {
            throw new BizException("会计期间未开放：" + period);
        }
    }

    private String resolvePeriod(LocalDate voucherDate) {
        if (voucherDate == null) {
            throw new BizException("凭证日期不能为空");
        }
        return voucherDate.getYear() + "-" + String.format("%02d", voucherDate.getMonthValue());
    }

    /**
     * 生成下一个凭证号。
     * <p>
     * 凭证号按「记-YYYYMM-NNNN」组织，序号固定 4 位宽度（超过 9999 自然扩展）。
     * 取号依据数据库中的最大序号（数值排序）计算，并发场景下由唯一键兜底：
     * 保存时若撞号则重新取号重试。
     */
    private String nextVoucherNo(String period) {
        Integer maxSeq = baseMapper.selectMaxSeqByPeriod(period);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return "记-" + period.replace("-", "") + "-" + String.format("%04d", nextSeq);
    }

    private VoucherVO toVO(FiVoucher voucher) {
        VoucherVO vo = new VoucherVO();
        BeanUtils.copyProperties(voucher, vo);
        return vo;
    }

    private static class VoucherDraft {
        private final FiVoucher voucher;
        private final List<FiVoucherEntry> entries;

        private VoucherDraft(FiVoucher voucher, List<FiVoucherEntry> entries) {
            this.voucher = voucher;
            this.entries = entries;
        }

        private FiVoucher getVoucher() {
            return voucher;
        }

        private List<FiVoucherEntry> getEntries() {
            return entries;
        }
    }
}








