package com.erp.common.util;

import com.erp.common.exception.BizException;
import com.erp.security.datascope.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务单号生成器。
 * <p>
 * 单号形如 {@code CG-202610-0001}：前缀 + 期间 + 4 位序号。
 * <p>
 * 序号来自 {@code sys_serial_no} 流水表的原子自增，而不是「查最大号 + 1」，
 * 以此消除三个真实缺陷：
 * <ul>
 *   <li>并发下单时两个事务取到同一个号，插入撞唯一键；</li>
 *   <li>单据软删除后重新取号会重用旧号，审计链条断裂；</li>
 *   <li>取号查询被部门数据范围裁剪，不同部门取到相同序号。</li>
 * </ul>
 * 流水表按「前缀 + 期间」维度自增，取号与数据权限无关；
 * 软删除的单据号不会退回，序号只增不减，保证单号可追溯。
 */
@Component
@RequiredArgsConstructor
public class SerialNoGenerator {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 生成下一个单号。
     *
     * @param prefix 单号前缀，如 {@code CG}
     * @param period 期间标识，通常是 yyyyMM
     * @return 完整单号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String next(String prefix, String period) {
        long seq = nextSeq(prefix, period);
        return String.format("%s-%s-%04d", prefix, period, seq);
    }

    /**
     * 取下一个序号。
     * <p>
     * 用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 实现原子自增：
     * 首次取号插入一行，后续取号由唯一键命中并自增，全程不出现竞态窗口。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long nextSeq(String prefix, String period) {
        // 取号属于系统级约束查询，不受部门数据范围裁剪
        return DataScopeContext.ignoringScope(() -> {
            jdbcTemplate.update(
                    "INSERT INTO sys_serial_no (`prefix`, `period`, curr_seq) VALUES (?, ?, 1) "
                            + "ON DUPLICATE KEY UPDATE curr_seq = curr_seq + 1, update_time = NOW()",
                    prefix, period);
            Long seq = jdbcTemplate.queryForObject(
                    "SELECT curr_seq FROM sys_serial_no WHERE `prefix` = ? AND `period` = ?",
                    Long.class, prefix, period);
            if (seq == null) {
                throw new BizException("业务单号生成失败");
            }
            return seq;
        });
    }

    /**
     * 把流水表推进到不小于指定序号的位置。
     * <p>
     * 用于导入历史数据或迁移后对齐：保证新生成的单号不会与已存在的历史单号重复。
     *
     * @param prefix 单号前缀
     * @param period 期间标识
     * @param seq    已用到的最大序号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void reserve(String prefix, String period, long seq) {
        if (seq <= 0) {
            return;
        }
        DataScopeContext.ignoringScope(() -> {
            jdbcTemplate.update(
                    "INSERT INTO sys_serial_no (`prefix`, `period`, curr_seq) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE curr_seq = GREATEST(curr_seq, ?), update_time = NOW()",
                    prefix, period, seq, seq);
            return null;
        });
    }

    /**
     * 查询指定前缀+期间当前已分配到的序号。
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long currentSeq(String prefix, String period) {
        Long seq = jdbcTemplate.queryForObject(
                "SELECT curr_seq FROM sys_serial_no WHERE `prefix` = ? AND `period` = ?",
                Long.class, prefix, period);
        return seq == null ? 0L : seq;
    }

    /**
     * 记录一次单号分配，便于审计排查。
     */
    void touch(String prefix, String period) {
        jdbcTemplate.update("UPDATE sys_serial_no SET update_time = ? WHERE `prefix` = ? AND `period` = ?",
                LocalDateTime.now(), prefix, period);
    }
}
