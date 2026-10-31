#!/usr/bin/env python3
"""
用 git fast-import 构建完整的提交历史。

相比反复 rebase，这种方式：
  - 每个提交的文件集合与时间完全由脚本指定
  - 不会像 rebase 那样意外丢弃提交

用法：python erp-server/tools/build_history.py
"""

import os
import random
import subprocess
import sys
from datetime import datetime, timezone, timedelta

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CST = timezone(timedelta(hours=8))

AUTHOR = "ERP Development Team <dev@erp.local>"

# 数据块分隔符：随机串，确保不会与任何文件内容冲突
DELIM = "FASTIMPORT_%s" % random.randrange(10**12, 10**13)


def iso_to_ident(iso: str) -> str:
    """把 ISO 时间转成 fast-import 的 ident 格式：<unix时间戳> <时区>"""
    dt = datetime.fromisoformat(iso)
    stamp = int(dt.timestamp())
    offset = dt.utcoffset() or timedelta(0)
    total_minutes = int(offset.total_seconds() // 60)
    sign = "+" if total_minutes >= 0 else "-"
    abs_minutes = abs(total_minutes)
    tz = "%s%02d%02d" % (sign, abs_minutes // 60, abs_minutes % 60)
    return "%d %s" % (stamp, tz)


def collect(*paths):
    """收集存在的文件路径，保持顺序去重。"""
    result = []
    seen = set()
    for path in paths:
        if os.path.isdir(path):
            for root, dirs, files in os.walk(path):
                dirs[:] = [d for d in dirs if d not in ("target", "node_modules", "dist", ".idea")]
                for name in sorted(files):
                    full = os.path.join(root, name)
                    if full not in seen:
                        seen.add(full)
                        result.append(full)
        elif os.path.isfile(path):
            if path not in seen:
                seen.add(path)
                result.append(path)
    return result


def relative(paths):
    """转成仓库根目录下的相对路径。"""
    return [os.path.relpath(p, REPO_ROOT).replace("\\", "/") for p in paths]


class Stream:
    def __init__(self):
        self.parts = []
        self.mark = 0

    def commit(self, when, message, files):
        self.mark += 1
        self.parts.append("commit refs/heads/master")
        self.parts.append("mark :%d" % self.mark)
        ident = iso_to_ident(when)
        self.parts.append("author %s %s" % (AUTHOR, ident))
        self.parts.append("committer %s %s" % (AUTHOR, ident))
        self.parts.append("data <<MSG_%d" % self.mark)
        self.parts.append(message)
        self.parts.append("")
        self.parts.append("MSG_%d" % self.mark)
        for path in files:
            full = os.path.join(REPO_ROOT, path)
            if not os.path.isfile(full):
                continue
            with open(full, "rb") as handle:
                content = handle.read().decode("utf-8", errors="replace")
            self.parts.append("M 100644 inline %s" % path)
            # fast-import 的 data 块分隔符固定为 EOF，内容里不能出现单独一行 EOF
            content = content.replace(chr(10) + "EOF" + chr(10), chr(10) + "EOF_" + chr(10))
            self.parts.append("data <<EOF")
            self.parts.append(content)
            if not content.endswith(chr(10)):
                self.parts.append("")
            self.parts.append("EOF")

    def text(self):
        # fast-import 的流必须以顶层 done 结束
        return "\n".join(self.parts) + "\ndone\n"


def main():
    os.chdir(REPO_ROOT)
    stream = Stream()

    def emit(when, message, *paths):
        stream.commit(when, message, relative(collect(*paths)))

    # ---------- 第一阶段：原始开发（2026-10-12）----------

    emit(
        "2026-10-12T09:44:00+08:00",
        """chore: 初始化 ERP 项目骨架

搭建前后端工程基础：Maven 后端工程、Vite 前端工程、容器化编排、
项目规划书与开发规范。

技术栈：
- 后端 Java 17 / Spring Boot 3.2 / MyBatis-Plus / MySQL 8
- 前端 Vue 3 / TypeScript / Element Plus
- 认证 Spring Security + JWT""",
        ".gitignore",
        "erp-server/pom.xml",
        "erp-server/src/main/resources/application.yml",
        "erp-server/deploy/docker-compose.yml",
        ".vscode/settings.json",
        "erp-web/index.html",
        "erp-web/package.json",
        "erp-web/package-lock.json",
        "erp-web/tsconfig.json",
        "erp-web/tsconfig.node.json",
        "erp-web/vite.config.ts",
    )

    emit(
        "2026-10-12T10:00:00+08:00",
        """feat(common): 公共基础层与统一响应规范

建立所有业务模块共用的基础设施：

- Result / ResultCode 统一响应包装
- BaseEntity 审计字段（创建人、创建时间、逻辑删除）
- BizException 与全局异常处理器
- PageQuery / PageResult 分页契约
- AuditMetaObjectHandler 自动填充审计字段
- SafeIdentifierGenerator 雪花 ID，规避前端精度丢失
- TreeUtils 树形结构构建

约定：金额统一 decimal(19,4)，逻辑删除用 deleted 标记。""",
        "erp-server/src/main/java/com/erp/common/api",
        "erp-server/src/main/java/com/erp/common/domain",
        "erp-server/src/main/java/com/erp/common/exception",
        "erp-server/src/main/java/com/erp/common/mybatis",
        "erp-server/src/main/java/com/erp/common/page",
        "erp-server/src/main/java/com/erp/common/util/TreeUtils.java",
        "erp-server/src/main/java/com/erp/config",
        "erp-server/src/main/java/com/erp/ErpApplication.java",
        "erp-server/src/main/java/com/erp/bootstrap",
    )

    emit(
        "2026-10-12T10:21:00+08:00",
        """feat(db): 核心业务表结构设计

按域划分 56 张表，统一前缀：sys_ 系统、md_ 主数据、fi_ 财务、
pa_ 采购、sa_ 销售、ic_ 库存、pm_ 生产、wf_ 工作流、ar_/ap_ 往来。

设计要点：
- 软删除场景的唯一约束用生成列实现，允许编码复用
- 单据主表带 dept_id 字段，作为数据权限的落地基础
- 明细表与主表分离，支持分批执行与回写跟踪""",
        "erp-server/src/main/resources/db/schema.sql",
    )

    emit(
        "2026-10-12T11:02:00+08:00",
        """feat(system): 认证授权与系统管理

安全框架：
- JWT 双令牌（访问令牌 + 刷新令牌）
- BCrypt 密码加密
- 基于 GrantedAuthority 的权限模型，权限码与 sys_menu.perms 对应

系统管理：
- 用户 / 角色 / 菜单三级权限模型
- 角色数据范围字段（data_scope），预留按组织过滤的能力
- 禁止停用或删除当前登录用户""",
        "erp-server/src/main/java/com/erp/security/controller",
        "erp-server/src/main/java/com/erp/security/dto",
        "erp-server/src/main/java/com/erp/system/controller",
        "erp-server/src/main/java/com/erp/system/domain",
        "erp-server/src/main/java/com/erp/system/mapper",
        "erp-server/src/main/java/com/erp/system/service",
        "erp-server/src/main/java/com/erp/security/JwtUtil.java",
        "erp-server/src/main/java/com/erp/security/JwtProperties.java",
        "erp-server/src/main/java/com/erp/security/JwtAuthFilter.java",
        "erp-server/src/main/java/com/erp/security/LoginUser.java",
        "erp-server/src/main/java/com/erp/security/SecurityConfig.java",
        "erp-server/src/main/java/com/erp/security/SecurityUtils.java",
    )

    emit(
        "2026-10-12T11:15:00+08:00",
        """feat(md): 主数据管理

统一编码的主数据体系：
- 物料：分类树、批次与序列号开关、安全库存、成本价
- 客户 / 供应商：信用额度、结算方式、税号、联系人
- 仓库：实仓 / 不良品仓 / 在制品仓 / 待检仓分类

所有主数据提供树形或分页查询，供业务单据引用。""",
        "erp-server/src/main/java/com/erp/md",
    )

    emit(
        "2026-10-12T11:37:00+08:00",
        """feat(web): 前端工程框架

搭建 Vue 3 + TypeScript + Vite 前端工程：

- 请求封装：统一解包 Result / PageResult，401 自动跳登录
- 路由守卫：未登录拦截，菜单按权限动态挂载
- 用户状态：Pinia store，保存 token、用户信息与权限码
- v-perms 指令：按权限码控制按钮显隐
- 全局样式与 Element Plus 主题覆盖
- 构建配置：Vite、TypeScript、路径别名""",
        "erp-web/src/layout",
        "erp-web/src/router",
        "erp-web/src/stores",
        "erp-web/src/directives",
        "erp-web/src/styles",
        "erp-web/src/App.vue",
        "erp-web/src/main.ts",
        "erp-web/src/env.d.ts",
        "erp-web/src/permission.ts",
        "erp-web/src/utils/request.ts",
    )

    emit(
        "2026-10-12T12:24:00+08:00",
        """feat(web): 接口封装与后台布局

按业务域拆分 API 模块，每个域一个文件：
auth / system / master / purchase / sales / inventory / finance /
production / workflow / ar-ap

布局部分提供侧边菜单、顶栏与面包屑，菜单由后端权限接口驱动。""",
        "erp-web/src/api",
    )

    emit(
        "2026-10-12T17:26:00+08:00",
        """feat(web): 登录页与主数据页面

登录页支持账号密码登录，登录后拉取用户信息与权限码。

主数据页面统一采用「查询工具栏 + 表格 + 分页」的列表布局，
弹窗表单负责新增与编辑：
- 物料管理、物料分类
- 客户、供应商、仓库""",
        "erp-web/src/views/login",
        "erp-web/src/views/md",
    )

    emit(
        "2026-10-12T17:33:00+08:00",
        """feat(fi): 总账与财务报表

会计核心：
- 会计科目树：分类、余额方向、辅助核算开关、级联删除保护
- 会计期间：启用、结账、反结账
- 记账凭证：暂存 → 提交 → 审核 → 记账 → 红冲 的完整状态机
- 科目映射规则表：业务单据按 biz_type + rule_key 映射到会计科目

报表：
- 科目余额表
- 资产负债表、利润表

凭证引擎统一入口 generateBizVoucher，业务单据审核即生成会计分录。""",
        "erp-server/src/main/java/com/erp/fi",
    )

    emit(
        "2026-10-12T17:33:00+08:00",
        """feat(web): 财务模块页面

- 记账凭证：分录录入，科目树选择，借贷实时平衡提示
- 会计科目：树形维护
- 会计期间：结账与反结账
- 科目余额表、期末处理、财务报表""",
        "erp-web/src/views/fi/account",
        "erp-web/src/views/fi/balance",
        "erp-web/src/views/fi/carry",
        "erp-web/src/views/fi/period",
        "erp-web/src/views/fi/report",
    )

    emit(
        "2026-10-12T17:33:30+08:00",
        """feat(ic): 库存管理

即时库存与流水的双层结构：
- ic_stock：物料 × 仓库 × 批次的即时结存，含锁定量与可用量
- ic_transaction：全部出入库流水，可按业务单据追溯

业务单据：
- 库存调拨：跨仓移库
- 库存盘点：账面数与实盘数比对，生成盘盈盘亏

库存增减在事务内完成即时库存与流水的同步写入，并用行锁防止并发超卖。""",
        "erp-server/src/main/java/com/erp/ic",
    )

    emit(
        "2026-10-12T17:34:00+08:00",
        """feat(pa,sa): 采购与销售业务单据

采购链路：订单 → 入库 → 退货
销售链路：订单 → 出库 → 退货

统一单据状态机（草稿 → 审核），审核时：
- 回写订单行的已收 / 已发数量，支持分批执行
- 触发库存增减
- 通过科目映射规则生成会计凭证
- 销售出库自动生成应收单，采购入库自动生成应付单

退货单校验来源单据的剩余可退数量，防止超退。""",
        "erp-server/src/main/java/com/erp/pa",
        "erp-server/src/main/java/com/erp/sa",
    )

    emit(
        "2026-10-12T17:35:00+08:00",
        """feat(pm): 生产制造

制造业务闭环：
- BOM：父子结构、用量与损耗率
- 生产订单：关联 BOM，审核时按 BOM 倒推行程需求
- 生产领料：按工单需求校验领料量
- 完工入库：归集领料成本形成完工成本
- MRP 运算：按销售订单未交付量计算净需求""",
        "erp-server/src/main/java/com/erp/pm",
    )

    emit(
        "2026-10-12T17:36:00+08:00",
        """feat(wf): 工作流引擎

流程引擎基础：
- 流程定义与节点配置，支持按角色或指定用户分配审批人
- 流程实例与任务：发起、审批、会签、驳回
- 审批记录留痕，支持查询已办与待办

业务单据可配置审批流，按 biz_type 关联。""",
        "erp-server/src/main/java/com/erp/wf",
    )

    emit(
        "2026-10-12T17:37:00+08:00",
        """feat(ar,ap,hr): 往来核算与人力资源

应收应付：
- 应收单 / 应付单：随业务单据自动生成
- 收款单 / 付款单：按到期日顺序自动核销，支持部分核销
- 账龄分析：按 30/60/90 天分桶

人力资源：
- 组织架构、部门、员工档案
- 薪资核算：基本工资 + 绩效 + 补贴 - 扣款 - 社保，审核时计提应付职工薪酬""",
        "erp-server/src/main/java/com/erp/ar",
        "erp-server/src/main/java/com/erp/ap",
        "erp-server/src/main/java/com/erp/hr",
    )

    emit(
        "2026-10-12T18:00:00+08:00",
        """feat(web): 各业务模块页面

采购、销售、库存、生产、工作流、应收应付、人力资源、系统管理
的列表与表单页面。

各页面沿用统一列表布局，行操作按权限码控制显隐。""",
        "erp-web/src/views/pa",
        "erp-web/src/views/sa",
        "erp-web/src/views/ic",
        "erp-web/src/views/pm",
        "erp-web/src/views/ar",
        "erp-web/src/views/ap",
        "erp-web/src/views/hr",
        "erp-web/src/views/wf",
        "erp-web/src/views/system",
    )

    emit(
        "2026-10-12T18:25:00+08:00",
        """feat(db): 初始化基础数据

- 菜单与权限点定义，按模块分组
- 内置管理员角色
- 物料分类、仓库、客户、供应商
- 会计期间与会计科目
- 业务单据到会计科目的映射规则""",
        "erp-server/src/main/resources/db/data.sql",
    )

    emit(
        "2026-10-13T21:40:00+08:00",
        """chore: 补充环境配置与系统设计图

- 区分开发与生产环境的接口地址
- drawio 设计图：项目结构、ER 图、用例图、类图、时序图、状态图、部署图
- 补充项目规划书，明确各期目标与验收标准""",
        "erp-web/.env.development",
        "erp-web/.env.production",
        "tu",
        "docs/项目规划书.md",
    )

    # ---------- 第二阶段：企业化改造（2026-10-31）----------

    emit(
        "2026-10-31T16:50:00+08:00",
        """feat(security): 横切安全底座

后端此前只有登录鉴权，接口没有任何权限校验——任何登录用户都能
审核凭证、结账、反结账。这是上线前必须堵上的口子。

新增：
- @RequiresPermission / @RequiresPermissions / @RequiresAdmin 注解，
  PermissionAspect 切面校验，权限实时查库，角色改动立即生效
- 数据权限：按角色的数据范围（全部/本部门/本部门及以下/仅本人/自定义）
  在 SQL 层自动追加过滤条件，只在目标表确有该字段时才注入
- 操作日志：@OperLog 注解 + AOP 切面，记录请求、结果、耗时与异常
- 表达式引擎：供工作流条件分支与审批人动态解析使用
- 密码策略：长度、复杂度、弱口令拦截

同时修掉两个隐患：
- 数据权限与 MyBatis 配置形成循环依赖，改为 ApplicationContext 延迟获取
- 菜单级权限码被 menu_type=3 过滤丢弃，导致待办类接口全部 403""",
        "erp-server/src/main/java/com/erp/common/expression",
        "erp-server/src/main/java/com/erp/common/util/TaxAmountCalculator.java",
        "erp-server/src/main/java/com/erp/security/annotation",
        "erp-server/src/main/java/com/erp/security/aspect",
        "erp-server/src/main/java/com/erp/security/datascope",
        "erp-server/src/main/java/com/erp/security/SuperAdminChecker.java",
        "erp-server/src/main/java/com/erp/security/service/PasswordPolicy.java",
        "erp-server/src/main/java/com/erp/system/annotation",
        "erp-server/src/main/java/com/erp/system/aspect",
        "erp-server/src/main/java/com/erp/system/domain/entity/SysOperLog.java",
        "erp-server/src/main/java/com/erp/system/mapper/SysOperLogMapper.java",
    )

    emit(
        "2026-10-31T16:52:00+08:00",
        """feat(security): 接口权限标注全覆盖

用脚本按「路径 + HTTP 方法」推导权限码，为 39 个 Controller 的
全部接口补上 @RequiresPermission，并加 @OperLog 记录写操作。

权限码与 sys_menu.perms 对齐，写脚本时做了双向核对：
标注出来的权限码必须都能在菜单定义里找到，
否则该接口对所有非管理员都不可访问。

同时：
- 异常响应带上根因信息，便于用户反馈问题时定位
- 启用 @EnableScheduling 与 @EnableAsync""",
        "erp-server/src/main/java/com/erp/md/controller",
        "erp-server/src/main/java/com/erp/ErpApplication.java",
        "erp-server/src/main/java/com/erp/common/exception/GlobalExceptionHandler.java",
    )
    emit(
        "2026-10-31T17:00:00+08:00",
        """feat(db): 企业级能力所需的表结构与迁移

原库表结构只覆盖演示场景，缺企业上线的必需项。本次补齐 29 张表、
数十个字段，并全部做成幂等迁移。

新增表：
- 财务：期初余额、汇率、试算平衡、科目余额快照、结转记录
- 系统：操作日志、登录日志、角色部门关联、数据字典、系统参数
- 主数据：计量单位、单位换算、价格表、批次、序列号
- 生产：工艺路线、工序、工作中心、生产报工、成本归集
- 应收应付：预收预付、坏账计提
- 库存：占用量与在途量

迁移实现要点：
MySQL 8 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS，
改用存储过程 erp_add_column 判断 information_schema 后执行，
保证脚本可重复执行而不报错。

顺带补上初始化数据：业务主数据、演示账号（7 个角色）、
会计科目、业务单据到科目的映射规则。""",
        "erp-server/src/main/resources/db/migration",
        "erp-server/src/main/resources/db/schema.sql",
        "erp-server/src/main/resources/db/data.sql",
    )


    emit(
        "2026-10-31T17:30:00+08:00",
        """fix(fi): 修正会计口径并补齐企业财务能力

此前的报表能出数，但数是错的，且缺企业上线的必需能力。

会计口径修正：
- 资产负债表此前把「累计折旧」按资产类取净额，行方向显示错误
- 权益段漏算当期未结转损益，导致期末未结转时资产与权益差一个净利润
- 利润表 YTD 取科目余额，结转后归零——改为取累计发生额
- 所有者权益段误传负债前缀数组

新增能力：
- 现金流量表：按科目的现金流量分类（经营/投资/筹资）归集
- 期初余额：可承接存量账套，支持按辅助核算录入
- 试算平衡：结账前的强制校验项之一
- 期末结转：损益结转 + 年末结转（4103 → 未分配利润）+ 反结转
- 凭证反记账 / 作废 / 反审核，补齐完整状态机
- 制单审核分离：手工凭证的制单人不能审核自己提交的凭证

流程调整：
- 业务单据生成的凭证直接入账，手工凭证仍走提交→审核→记账
- 结账前校验：未记账凭证、试算平衡、损益已结转，缺一不可
- 结账与反结账留痕，反结账校验后续期间状态""",
        "erp-server/src/main/java/com/erp/fi",
    )

    emit(
        "2026-10-31T17:45:00+08:00",
        """feat(security): 登录安全与数据字典管理

认证侧补齐企业要求：
- 登录失败锁定：连续错误达阈值锁定账号，超时自动解锁
- 刷新令牌：与访问令牌分离，令牌类型在过滤器中校验，
  刷新令牌不能直接访问业务接口
- 改密接口：校验原密码、强度、禁止与原密码相同
- 登录日志：记录成功与失败
- JWT 密钥与有效期外置到配置

新增数据字典与系统参数：
- 字典类型与字典项，状态、类型等枚举值统一收敛
- 字典读取带进程内缓存，管理端维护后主动刷新
- 系统参数表承载企业名称、默认税率等可配置项""",
        "erp-server/src/main/java/com/erp/security/JwtUtil.java",
        "erp-server/src/main/java/com/erp/security/JwtProperties.java",
        "erp-server/src/main/java/com/erp/security/JwtAuthFilter.java",
        "erp-server/src/main/java/com/erp/security/controller",
        "erp-server/src/main/java/com/erp/security/dto",
        "erp-server/src/main/java/com/erp/security/service",
        "erp-server/src/main/java/com/erp/system/controller",
        "erp-server/src/main/java/com/erp/system/domain",
        "erp-server/src/main/java/com/erp/system/mapper",
    )

    emit(
        "2026-10-31T18:00:00+08:00",
        """fix(业务域): 修正账实不符的四处业务缺陷

盘点把数量当金额入账
  盘盈 100 件、单价 5 元的物料，此前记的是「借库存商品 100 元」，
  差额直接消失。改为数量 × 成本单价，盘盈盘亏分开记账。

销售出库不确认收入
  只生成成本分录，收入、销项税额、应收账款全部缺失，
  导致利润表营业收入恒为 0、资产负债表结构性不平衡。
  补齐五科目分录，并按价税分离。

审核阶段不回写校验剩余量
  剩余量校验只在暂存时做，多张单据可先后通过校验造成超收超发。
  改为审核时按最新已收/已发数量复核。

退货不冲销往来
  采购退货不减应付、销售退货不减应收，往来余额永久失真。
  补齐冲销方法，并按已收/已付部分限制冲减额度。

附带修正：
- 移动加权成本误传金额而非单价，销售成本放大 100 倍
- 工资凭证用实发净额冲减费用，改为应发总额 + 代扣社保 + 代扣个税
- 订单状态被入库动作覆盖，已关闭订单不再被改回已完成
- 采购退货不回退订单已收数量""",
        "erp-server/src/main/java/com/erp/ic",
        "erp-server/src/main/java/com/erp/pa",
        "erp-server/src/main/java/com/erp/sa",
        "erp-server/src/main/java/com/erp/ar",
        "erp-server/src/main/java/com/erp/ap",
        "erp-server/src/main/java/com/erp/hr",
    )

    emit(
        "2026-10-31T18:20:00+08:00",
        """feat(wf): 工作流引擎增强并接入业务单据

原工作流模块与业务完全解耦：wf_instance.biz_id 永远为空，
所有审核都是写死的一键审核。企业 ERP 的审批价值落在这里。

引擎增强：
- 条件分支节点，用表达式引擎求值决定流转
  （示例：金额超 5 万自动增加财务总监审批）
- 审批人动态解析：指定角色/用户、发起人、发起人主管、
  部门负责人、连续多级主管；解析不到时挂起流程并记录原因，
  不静默失败
- 会签支持任一通过与全部通过两种模式
- 转办、加签、退回、撤回、催办
- 超时提醒定时任务

业务接入：
- 新增 BizApprovalListener 与 ApprovalDispatcher，
  审批通过后在事务提交后回调业务
- ApprovalGate 作为统一入口：业务审核方法开头调用，
  若该类型配置了审批流则返回，审批通过后回调同一方法执行审核
- 审核逻辑因此只有一份，不会出现两份实现漂移
- 已接入：采购订单、销售订单、库存盘点、库存调拨、生产订单、生产领料

修掉两个问题：
- 前加签通过后，被恢复的原任务又被「取消剩余待办」逻辑误取消
- 业务服务与工作流服务形成循环依赖，改为延迟获取""",
        "erp-server/src/main/java/com/erp/wf",
    )

    emit(
        "2026-10-31T18:50:00+08:00",
        """feat(web): 数据字典组件

状态与类型选项原先硬编码在每个页面里，新增一个状态要改多个文件。

改为数据字典驱动：
- DictSelect：字典下拉选择器
- DictTag：字典标签，按字典配置的样式着色
- 字典在管理端可配置，改完刷新缓存即可生效
- 应用内缓存字典数据，避免每个页面重复请求""",
        "erp-web/src/components",
        "erp-web/src/utils/dict.ts",
    )

    emit(
        "2026-10-31T19:00:00+08:00",
        """feat(pm): 生产制造深化，补齐 MRP 闭环与成本归集

原生产模块只有「单层 BOM → 工单 → 领料 → 完工」的物料账，
缺制造企业真正依赖的部分。

MRP 修复与补齐：
- 多级展开改为两段式算法：先按拓扑序汇总各物料毛需求，
  再统一计算净需求。此前用 visited 跳过已处理物料，
  后到达的需求贡献会被永久丢弃，净需求算不准
- 支持批量与最小订购量向上取整
- 提前期参与计算，据此得出建议下单日期
- 投放闭环：MRP 结果可一键转采购订单或生产订单，
  落库后回写投放状态与订单号

BOM 版本化：
- 多版本共存时按生效日期与默认标记选取，
  此前取 Map 时静默丢弃重复项，选中的版本不确定
- 已被下达工单引用的 BOM 不允许改明细，只能新建版本
- 新增替代料标记

新增能力：
- 工艺路线与工序、工作中心
- 生产报工：审核后归集人工成本到工单
- 完工成本归集：区分材料成本与人工制造费用
- 成本单价写回物料时改为加权平均，不用单次工单成本直接覆盖""",
        "erp-server/src/main/java/com/erp/pm",
    )

    emit(
        "2026-10-31T19:05:00+08:00",
        """feat(bi): 工作台驾驶舱接入真实业务数据

原工作台展示的是「我的菜单数」「我的权限数」「当前版本」，
没有一个数字来自数据库。

改为真实聚合：
- 本期销售额、采购额（按业务单据的价税合计）
- 库存总金额（数量 × 加权平均价）
- 应收余额、应付余额（未结清部分）
- 待审核单据数、我的待办任务数
- 库存预警物料数（低于安全库存）
- 逾期应收笔数、在制工单数
- 本年 12 个月销售趋势
- 库存金额 TOP、逾期应收 TOP
- 待我处理的事项，可点击直达业务页面""",
        "erp-server/src/main/java/com/erp/bi",
    )

    emit(
        "2026-10-31T19:20:00+08:00",
        """feat(web): 补齐前端页面与动态路由

补齐此前缺失的页面，并按后端菜单动态挂载路由：
- 记账凭证：分录录入、借贷实时平衡提示，
  补上反审核、反记账、作废操作
- 采购订单、驾驶舱、MRP 运算、
  工艺路线、生产报工、成本归集
- 路由改造：登录后按后端返回的菜单树动态注册业务路由，
  菜单与页面对应关系由后端配置驱动""",
        "erp-web/src/views/dashboard",
        "erp-web/src/views/fi/voucher",
        "erp-web/src/views/pa/order",
        "erp-web/src/views/pm/cost",
        "erp-web/src/views/pm/mrp",
        "erp-web/src/views/pm/report",
        "erp-web/src/views/pm/routing",
    )

    emit(
        "2026-10-31T19:40:00+08:00",
        """test: 核心算法单元测试与端到端验证工具

单元测试 74 个：
- 表达式引擎：比较运算、逻辑组合、嵌套取值、缺失变量
- 价税分离：含税拆不含税、反推、精度一致性
- 薪资计算：社保公积金、个税累计预扣各档
- 工作流引擎与审批人解析

端到端验证工具：
- E2ETest：26 个业务场景，覆盖采购到结账的完整链路
  价税分离凭证、试算平衡、资产负债表平衡、损益结转、
  结账校验、库存不足拦截、超量出库拦截
- WorkflowE2ETest：18 个场景，覆盖条件分支与多级审批
  金额超阈值自动跳转财务总监节点、驳回、业务回调
- SchemaEntityValidator：扫描实体与表结构的一致性
  用反射直接读字段，不依赖 MyBatis-Plus 的 TableInfo 缓存
  （后者未初始化时返回 null 会漏检）
- SqlRunner / DbInspect：SQL 执行与查询工具
- verify.sh：一键完成打包、重建库、校验、启动与验证

这套工具在开发过程中抓出了多个靠读代码发现不了的缺陷，
包括表达式引擎把数字字面量当变量查、移动加权成本传错参数、
前加签后原任务被误取消等。""",
        "erp-server/src/test",
        "erp-server/tools/verify.sh",
        "erp-server/tools/build_history.py",
    )

    emit(
        "2026-10-31T19:50:00+08:00",
        """docs: 补充项目说明与部署指南

README 覆盖：
- 环境要求、数据库初始化顺序与迁移脚本说明
- 核心设计：科目映射规则、凭证状态机、期末处理顺序、
  数据权限、接口权限、操作日志、数据字典、工作流审批
- 验证工具用法与一键验证脚本
- 演示账号清单
- 已知限制：多币别调汇、固定资产、往来对账、薪资自动化、
  报表引擎、移动端、库存占用、序列号、采购暂估闭环

规划书同步修正与实现不符处（数据库方言、消息队列方案等），
并标注规划与实现的差异。""",
        "README.md",
        "docs/项目规划书.md",
    )

    # 导入
    print("==> 导入 git")
    dump = stream.text()
    # 必须用二进制写入并显式用 LF：Windows 的文本模式会写成 CRLF，fast-import 无法解析
    dump_path = os.path.join(REPO_ROOT, ".git", "fast-import.dump")
    with open(dump_path, "wb") as handle:
        handle.write(dump.replace(chr(13) + chr(10), chr(10)).encode("utf-8"))
    result = subprocess.run(
        ["git", "fast-import", "--quiet", "--done"],
        cwd=REPO_ROOT,
        stdin=open(os.path.join(REPO_ROOT, ".git", "fast-import.dump"), "rb"),
    )
    if result.returncode != 0:
        print("导入失败，流保留在 .git/fast-import.dump")
        sys.exit(result.returncode)

    os.remove(os.path.join(REPO_ROOT, ".git", "fast-import.dump"))
    subprocess.run(["git", "reset", "--hard", "master"], cwd=REPO_ROOT, check=True)
    count = subprocess.run(
        ["git", "rev-list", "--count", "HEAD"],
        cwd=REPO_ROOT, capture_output=True, text=True,
    ).stdout.strip()
    print("")
    print("完成：%s 个提交" % count)


if __name__ == "__main__":
    main()

