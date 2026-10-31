package com.erp.security.datascope;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限内拦截器（MyBatis-Plus 扩展点）。
 * <p>
 * 在查询执行前，根据当前用户的数据范围，向 SQL 追加部门/创建人过滤条件。
 * 只处理单据主表（{@code SELECT ... FROM <主表>}），
 * 确保主表确实存在 dept_id / create_by 字段后才注入，避免 unknown column。
 * <p>
 * 解析器通过 ApplicationContext 延迟获取：拦截器由 MyBatis 配置类注册，
 * 若直接注入解析器会与 sqlSessionTemplate 形成循环依赖。
 */
@Slf4j
@Component
public class DataScopeInnerInterceptor implements InnerInterceptor, ApplicationContextAware {

    /** 参与数据权限的表（单据主表） */
    private static final Set<String> SCOPED_TABLES = new LinkedHashSet<>(List.of(
            "pa_order", "pa_instock", "pa_return",
            "sa_order", "sa_outstock", "sa_return",
            "ic_transfer", "ic_check",
            "pm_mo", "pm_pick", "pm_finish",
            "ar_receivable", "ar_receipt",
            "ap_payable", "ap_payment",
            "hr_salary"));

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }
        TableInfo tableInfo = resolveTableInfo(ms, parameter);
        if (tableInfo == null) {
            return;
        }
        String tableName = tableInfo.getTableName();
        if (tableName == null || !SCOPED_TABLES.contains(tableName.toLowerCase())) {
            return;
        }

        DataScopeContext.DataScopeInfo info = DataScopeContext.get();
        if (info == null) {
            info = resolveScope();
        }
        if (info == null || info.getScope() == DataScope.ALL) {
            return;
        }

        Expression condition = buildCondition(info, tableInfo);
        if (condition == null) {
            return;
        }

        try {
            String sql = boundSql.getSql();
            String rewritten = rewrite(sql, condition);
            if (rewritten != null && !rewritten.equals(sql)) {
                PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
                mpBoundSql.sql(rewritten);
                log.debug("数据权限改写 SQL [{}]: {}", tableName, rewritten);
            }
        } catch (Exception e) {
            log.warn("数据权限 SQL 改写失败 [{}]: {}", tableName, e.getMessage());
        }
    }

    /**
     * 从 Spring 上下文解析数据范围。查询发生时 sqlSessionTemplate 已就绪，不会形成循环依赖。
     */
    private DataScopeContext.DataScopeInfo resolveScope() {
        if (applicationContext == null) {
            return null;
        }
        try {
            DataScopeResolver resolver = applicationContext.getBean(DataScopeResolver.class);
            return resolver.resolveCurrent();
        } catch (Exception e) {
            log.warn("解析数据权限失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析本次查询对应的主表元信息。
     */
    private TableInfo resolveTableInfo(MappedStatement ms, Object parameter) {
        String id = ms.getId();
        if (id == null || !id.contains(".")) {
            return null;
        }
        String mapperClass = id.substring(0, id.lastIndexOf('.'));
        try {
            Class<?> mapperType = Class.forName(mapperClass, false,
                    Thread.currentThread().getContextClassLoader());
            TableInfo tableInfo = TableInfoHelper.getTableInfo(mapperType);
            if (tableInfo != null) {
                return tableInfo;
            }
        } catch (ClassNotFoundException ignored) {
            // 交由兜底逻辑处理
        }
        // BaseMapper 的 selectList / selectOne 等通用方法：走参数类型推断
        if (parameter instanceof QueryWrapper<?> wrapper && wrapper.getEntity() != null) {
            return TableInfoHelper.getTableInfo(wrapper.getEntity().getClass());
        }
        return null;
    }

    /**
     * 构造过滤条件；仅在主表确实存在对应字段时才返回条件。
     */
    private Expression buildCondition(DataScopeContext.DataScopeInfo info, TableInfo tableInfo) {
        boolean hasDeptId = hasField(tableInfo, "deptId");
        boolean hasCreateBy = hasField(tableInfo, "createBy");

        if (info.hasSelfRestriction()) {
            if (!hasCreateBy) {
                return null;
            }
            return new EqualsTo(new Column("create_by"), new LongValue(info.getUserId()));
        }
        if (info.hasDeptRestriction()) {
            if (!hasDeptId) {
                return null;
            }
            List<Long> deptIds = info.getDeptIds();
            if (deptIds.isEmpty()) {
                return new NotEqualsTo(new Column("dept_id"), new LongValue(0));
            }
            List<Expression> values = new ArrayList<>();
            for (Long deptId : deptIds) {
                values.add(new LongValue(deptId));
            }
            return new InExpression(new Column("dept_id"), new ExpressionList(values));
        }
        return null;
    }

    /**
     * 判断实体是否包含指定字段。
     */
    private boolean hasField(TableInfo tableInfo, String fieldName) {
        return tableInfo.getFieldList().stream()
                .anyMatch(field -> field.getProperty().equals(fieldName));
    }

    /**
     * 使用 JSqlParser 改写 SQL。
     */
    private String rewrite(String sql, Expression condition) throws Exception {
        var statement = CCJSqlParserUtil.parse(sql);
        if (!(statement instanceof Select select)) {
            return null;
        }
        SelectBody selectBody = select.getSelectBody();
        if (!(selectBody instanceof PlainSelect plainSelect)) {
            return null;
        }
        Expression where = plainSelect.getWhere();
        plainSelect.setWhere(where == null ? condition : new AndExpression(condition, where));
        return statement.toString();
    }
}








