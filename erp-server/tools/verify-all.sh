#!/usr/bin/env bash
# ============================================================
# ERP 全量功能验证
#
# 依次执行五层验证，覆盖接口可用性、业务逻辑、数据一致性与权限隔离：
#   1. ApiSmokeTest          全量接口巡检 + 越权拦截 + 未认证访问
#   2. WritePathTest         增删改查闭环 + 业务规则拒绝路径
#   3. DataScopeIsolationTest 数据权限隔离（同角色跨部门）
#   4. E2ETest               业务链路（采购→入库→凭证→记账→结转→结账）
#   5. WorkflowE2ETest       审批流（条件分支、多级审批、驳回）
#
# 用法：bash erp-server/tools/verify-all.sh
# ============================================================
set -e
cd "$(dirname "$0")/.."

SERVER_PORT=8487
JAR=target/erp-server-1.0.0-SNAPSHOT.jar
DB_URL="jdbc:mysql://localhost:3306/erp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
# 数据库凭据可用环境变量覆盖，与应用配置保持一致
DB_USER=${DB_USERNAME:-root}
DB_PASS=${DB_PASSWORD:-your_password}

# JWT 密钥与初始管理员口令由环境变量注入，代码与初始化脚本里没有公开默认值。
# 导出后既供应用启动使用，也供后续验证工具读取（测试从 ERP_ADMIN_PASSWORD 取口令）。
export JWT_SECRET=${JWT_SECRET:-verify-only-jwt-secret-key-at-least-32-bytes-long}
export ERP_ADMIN_PASSWORD=${ERP_ADMIN_PASSWORD:-Verify@123456}

TOTAL_PASS=0
TOTAL_FAIL=0
FAILED_SUITES=""

step() { echo ""; echo "=========== $* ==========="; }

# 停止正在运行的应用，避免打包时 jar 被占用
stop_app() {
  PID=$(netstat -ano 2>/dev/null | grep ":$SERVER_PORT.*LISTENING" | head -1 | awk '{print $5}' || true)
  if [ -n "$PID" ]; then
    taskkill //PID "$PID" //F > /dev/null 2>&1 || true
    sleep 3
  fi
}

start_app() {
  nohup java -jar "$JAR" > /tmp/erp-verify-all.log 2>&1 &
  for i in $(seq 1 45); do
    sleep 4
    CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$SERVER_PORT/auth/login" 2>/dev/null || echo 000)
    if [ "$CODE" = "200" ] || [ "$CODE" = "405" ]; then
      echo "应用已就绪"
      return 0
    fi
  done
  echo "应用启动失败，日志尾部："
  tail -30 /tmp/erp-verify-all.log
  return 1
}

run_suite() {
  local name="$1"
  local class="$2"
  local output
  # 单个套件失败不应中断整体验证
  output=$(java -cp "$CP" "$class" 2>&1 || true)
  echo "$output"
  local pass fail
  # 用机器可读的 SUMMARY 行统计，避免中文编码影响匹配
  pass=$(echo "$output" | grep -oE "SUMMARY pass=[0-9]+" | tail -1 | grep -oE "[0-9]+" || true)
  fail=$(echo "$output" | grep -oE "fail=[0-9]+" | tail -1 | grep -oE "[0-9]+" || true)
  pass=${pass:-0}
  fail=${fail:-0}
  echo "  [$name] 通过 $pass / 失败 $fail"
  TOTAL_PASS=$((TOTAL_PASS + pass))
  TOTAL_FAIL=$((TOTAL_FAIL + fail))
  if [ "$fail" != "0" ]; then
    FAILED_SUITES="$FAILED_SUITES $name"
  fi
}

step "准备"
stop_app
mvn -q test-compile -DskipTests > /dev/null 2>&1
mvn package -DskipTests 2>&1 | grep -E "^\[INFO\] BUILD" | head -1
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime > /dev/null 2>&1
CP="target/classes;target/test-classes;$(cat target/cp.txt)"
echo "编译打包完成"

step "重建数据库"
java -cp "$CP" com.erp.tools.SqlRunner "$DB_URL" "$DB_USER" "$DB_PASS" --drop
for f in src/main/resources/db/schema.sql \
         src/main/resources/db/migration/_helpers.sql \
         src/main/resources/db/migration/V1_1_0__erp_production_readiness.sql \
         src/main/resources/db/migration/V1_1_1__wf_enhancement.sql \
         src/main/resources/db/migration/V1_1_1__pm_enhancement.sql \
         src/main/resources/db/migration/V1_1_2__sys_dict.sql \
         src/main/resources/db/migration/V1_1_3__sys_serial_no.sql \
         src/main/resources/db/data.sql \
         src/main/resources/db/seed/demo_accounts.sql; do
  RESULT=$(java -cp "$CP" com.erp.tools.SqlRunner "$DB_URL" "$DB_USER" "$DB_PASS" "$f" 2>&1 | grep -E "^OK=" || echo "OK=0 FAILED=1")
  echo "  $(basename "$f"): $RESULT"
  echo "$RESULT" | grep -q "FAILED=0" || { echo "初始化失败：$f"; exit 1; }
done

step "实体与表结构一致性"
java -cp "$CP" com.erp.tools.SchemaEntityValidator "$DB_URL" "$DB_USER" "$DB_PASS" "$(pwd)/src/main/java" | tail -3

start_app

run_suite "接口巡检" com.erp.tools.ApiSmokeTest
run_suite "写路径与业务规则" com.erp.tools.WritePathTest
run_suite "数据权限隔离" com.erp.tools.DataScopeIsolationTest
run_suite "业务链路" com.erp.tools.E2ETest
run_suite "工作流" com.erp.tools.WorkflowE2ETest

step "单元测试"
mvn test 2>&1 | grep -E "^\[INFO\] Tests run: [0-9]+, Fail" | tail -1

echo ""
echo "=================================================="
echo "  验证项通过: $TOTAL_PASS"
echo "  验证项失败: $TOTAL_FAIL"
if [ -n "$FAILED_SUITES" ]; then
  echo "  失败套件:$FAILED_SUITES"
fi
echo "=================================================="
[ "$TOTAL_FAIL" = "0" ] || exit 1
