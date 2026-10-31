#!/usr/bin/env bash
# ============================================================
# ERP 一键验证脚本
#   1. 停止运行中的应用
#   2. 重新打包（确保 jar 含主清单）
#   3. 重置数据库并按序执行所有 SQL
#   4. 启动应用
#   5. 运行端到端验证
# 用法：bash tools/verify.sh [--skip-reset]
# ============================================================
set -e
cd "$(dirname "$0")/.."

SERVER_PORT=8487
JAR=target/erp-server-1.0.0-SNAPSHOT.jar
DB_URL="jdbc:mysql://localhost:3306/erp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
# 数据库凭据可用环境变量覆盖，与应用配置保持一致
DB_USER=${DB_USERNAME:-root}
DB_PASS=${DB_PASSWORD:-your_password}

step() { echo ""; echo "=========== $* ==========="; }

step "停止应用"
PID=$(netstat -ano 2>/dev/null | grep ":$SERVER_PORT.*LISTENING" | head -1 | awk '{print $5}' || true)
if [ -n "$PID" ]; then
  taskkill //PID "$PID" //F > /dev/null 2>&1 || true
  sleep 3
fi
echo "已停止"

step "编译打包"
mvn package -DskipTests 2>&1 | grep -E "^\[INFO\] BUILD|^\[ERROR\]" | head -5
unzip -p "$JAR" META-INF/MANIFEST.MF | grep -q "Start-Class" || { echo "jar 缺少主清单，终止"; exit 1; }
echo "jar 正常"

step "准备 classpath"
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime > /dev/null 2>&1
mvn -q test-compile -DskipTests > /dev/null 2>&1
CP="target/classes;target/test-classes;$(cat target/cp.txt)"

if [ "$1" != "--skip-reset" ]; then
  step "重置数据库并初始化"
  java -cp "$CP" com.erp.tools.SqlRunner "$DB_URL" "$DB_USER" "$DB_PASS" --drop
  for f in src/main/resources/db/schema.sql \
           src/main/resources/db/migration/_helpers.sql \
           src/main/resources/db/migration/V1_1_0__erp_production_readiness.sql \
           src/main/resources/db/migration/V1_1_1__wf_enhancement.sql \
           src/main/resources/db/migration/V1_1_1__pm_enhancement.sql \
           src/main/resources/db/migration/V1_1_2__sys_dict.sql \
           src/main/resources/db/migration/V1_1_3__sys_serial_no.sql \
           src/main/resources/db/data.sql; do
    RESULT=$(java -cp "$CP" com.erp.tools.SqlRunner "$DB_URL" "$DB_USER" "$DB_PASS" "$f" 2>&1 | grep -E "^OK=" || echo "OK=0 FAILED=1")
    echo "  $(basename "$f"): $RESULT"
    echo "$RESULT" | grep -q "FAILED=0" || { echo "  !! 初始化失败：$f"; exit 1; }
  done

  step "实体与表结构一致性校验"
  java -cp "$CP" com.erp.tools.SchemaEntityValidator "$DB_URL" "$DB_USER" "$DB_PASS" \
    "$(pwd)/src/main/java" | tail -3
fi

step "启动应用"
nohup java -jar "$JAR" > /tmp/erp-verify.log 2>&1 &
for i in $(seq 1 45); do
  sleep 4
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$SERVER_PORT/auth/login" 2>/dev/null || echo 000)
  if [ "$CODE" = "200" ] || [ "$CODE" = "405" ]; then
    echo "应用已就绪（${i}0 秒内）"
    break
  fi
done
if [ "$CODE" != "200" ] && [ "$CODE" != "405" ]; then
  echo "应用启动失败，日志尾部："
  tail -20 /tmp/erp-verify.log
  exit 1
fi

step "端到端业务验证"
java -cp "$CP" com.erp.tools.E2ETest








