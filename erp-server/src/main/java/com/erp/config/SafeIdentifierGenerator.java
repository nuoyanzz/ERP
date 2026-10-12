package com.erp.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * 前端安全的 ID 生成器。
 * <p>
 * MyBatis-Plus 默认雪花 ID 为 63 位（约 19 位十进制），超过 JS 的
 * Number.MAX_SAFE_INTEGER（2^53-1 = 9007199254740991），JSON 传到前端后
 * 会被解析成近似值（末位丢失）。例如数据库 2109590139713998849 前端得到
 * 2109590139713998800，导致新建记录的详情/编辑/删除全部使用错误 ID 而报
 * "记录不存在"。
 * </p>
 * <p>
 * 本生成器使用「毫秒时间戳(43bit) &lt;&lt; 10 | 同毫秒序列(10bit)」，
 * 产生的 ID 约 1.8e15，小于 2^53，可安全地被 JavaScript Number 精确表示。
 * </p>
 */
@Component
public class SafeIdentifierGenerator implements IdentifierGenerator {

    private static final long SEQ_BITS = 10L;
    private static final long SEQ_MASK = (1L << SEQ_BITS) - 1;

    private long lastMillis = -1L;
    private long lastSeq = -1L;

    @Override
    public synchronized Number nextId(Object entity) {
        long millis = System.currentTimeMillis();
        if (millis == lastMillis) {
            lastSeq = (lastSeq + 1) & SEQ_MASK;
            if (lastSeq == 0) {
                // 同一毫秒内序列号用尽，自旋等待下一毫秒
                while (millis == lastMillis) {
                    millis = System.currentTimeMillis();
                }
            }
        } else {
            lastSeq = 0L;
        }
        lastMillis = millis;
        return (millis << SEQ_BITS) | lastSeq;
    }
}








