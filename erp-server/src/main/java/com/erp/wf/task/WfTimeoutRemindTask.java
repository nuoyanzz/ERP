package com.erp.wf.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.domain.entity.WfTask;
import com.erp.wf.domain.entity.WfTaskRecord;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfTaskMapper;
import com.erp.wf.mapper.WfTaskRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批超时提醒定时任务。
 * <p>
 * 扫描已超过 {@code timeout_at} 的待办任务，写入审批流水并累加催办次数。
 * 当前项目没有消息中心，提醒以流水记录 + 日志的方式落地，后续接入通知能力时只需替换此处。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTimeoutRemindTask {

    /** 单个任务的最大提醒次数，超过后不再重复提醒，避免刷屏 */
    private static final int MAX_REMIND = 3;

    private final WfTaskMapper taskMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskRecordMapper recordMapper;

    /**
     * 每小时扫描一次超时任务。
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void scanTimeoutTasks() {
        List<WfTask> timeoutTasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getStatus, WfTask.STATUS_PENDING)
                .isNotNull(WfTask::getTimeoutAt)
                .le(WfTask::getTimeoutAt, LocalDateTime.now())
                .lt(WfTask::getRemindCount, MAX_REMIND));
        if (timeoutTasks == null || timeoutTasks.isEmpty()) {
            return;
        }
        for (WfTask task : timeoutTasks) {
            remind(task);
        }
        log.info("审批超时提醒扫描完成，共处理 {} 个任务", timeoutTasks.size());
    }

    /**
     * 对单个超时任务发送提醒。
     */
    private void remind(WfTask task) {
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || instance.getStatus() == null || instance.getStatus() != WfInstance.STATUS_RUNNING) {
            // 实例已结束，无需再提醒
            return;
        }
        int remindCount = (task.getRemindCount() == null ? 0 : task.getRemindCount()) + 1;
        WfTask update = new WfTask();
        update.setId(task.getId());
        update.setRemindCount(remindCount);
        taskMapper.updateById(update);

        WfTaskRecord record = new WfTaskRecord();
        record.setTaskId(task.getId());
        record.setInstanceId(task.getInstanceId());
        record.setNodeId(task.getNodeId() == null ? 0L : task.getNodeId());
        record.setAction(WfTaskRecord.ACTION_REMIND);
        record.setComment("审批超时，已提醒第 " + remindCount + " 次");
        record.setOperatorId(null);
        record.setOperateTime(LocalDateTime.now());
        recordMapper.insert(record);

        log.info("审批超时提醒：任务 {}（审批人 {}），第 {} 次提醒", task.getId(), task.getAssigneeId(), remindCount);
    }
}








