package com.erp.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志。记录对业务数据的写操作，供审计追溯。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    /** 操作模块，如 pa / sa / fi / ic */
    private String module;

    /** 操作类型：CREATE / UPDATE / DELETE / AUDIT / POST / CLOSE 等 */
    private String operType;

    /** 操作描述 */
    private String operDesc;

    /** 请求方法 */
    private String method;

    /** 请求 URL */
    private String requestUrl;

    /** 请求参数（截断存储） */
    private String params;

    /** 返回结果摘要（截断存储） */
    private String result;

    /** 操作人 ID */
    private Long operBy;

    /** 操作人账号 */
    private String operUsername;

    /** 操作人 IP */
    private String operIp;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 是否成功：1 成功 0 失败 */
    private Integer success;

    /** 失败信息 */
    private String errorMsg;
}








