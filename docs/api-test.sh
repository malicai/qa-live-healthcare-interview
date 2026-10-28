#!/usr/bin/env bash
# QA Live Healthcare 接口测试脚本(curl + Python JSON 断言)
# 用法:
#   bash docs/api-test.sh
#   API_BASE_URL=http://localhost:8082 bash docs/api-test.sh
set -u

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
PYTHON_BIN="${PYTHON_BIN:-python}"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    PYTHON_BIN="python3"
fi
if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    echo "错误: 需要 Python 3 用于解析 JSON 响应"
    exit 2
fi
if ! command -v curl >/dev/null 2>&1; then
    echo "错误: 需要 curl"
    exit 2
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

PASS=0
FAIL=0
TEST_SUFFIX="$(date +%Y%m%d%H%M%S)"
TEST_USERNAME="patient-test-${TEST_SUFFIX}"
SHORT_PASSWORD_USER="patient-short-${TEST_SUFFIX}"
NO_BIRTHDAY_USER="patient-nobd-${TEST_SUFFIX}"

json_assert() {
    local file="$1"
    local expression="$2"
    "$PYTHON_BIN" - "$file" "$expression" >/dev/null 2>&1 <<'PY'
import json
import sys

path, expression = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as handle:
    data = json.load(handle)

sys.exit(0 if eval(expression, {"data": data}) else 1)
PY
}

run_case() {
    local id="$1"
    local description="$2"
    local method="$3"
    local path="$4"
    local body_file="$5"
    local expected_status="$6"
    local assertion="$7"
    local out="$TMP_DIR/${id}.json"
    local status

    if [ -n "$body_file" ]; then
        status=$(curl -sS -o "$out" -w "%{http_code}" -X "$method" "${BASE_URL}${path}" \
            -H "Content-Type: application/json" --data-binary @"$body_file") || status="000"
    else
        status=$(curl -sS -o "$out" -w "%{http_code}" -X "$method" "${BASE_URL}${path}") || status="000"
    fi

    local ok=1
    [ "$status" = "$expected_status" ] || ok=0
    if [ "$ok" -eq 1 ] && [ -n "$assertion" ]; then
        json_assert "$out" "$assertion" || ok=0
    fi

    if [ "$ok" -eq 1 ]; then
        PASS=$((PASS + 1))
        printf "[PASS] %-4s %-6s %-42s -> HTTP %s\n" "$id" "$method" "$path" "$status"
    else
        FAIL=$((FAIL + 1))
        printf "[FAIL] %-4s %-6s %-42s -> HTTP %s (期望 %s)\n" "$id" "$method" "$path" "$status" "$expected_status"
        printf "       用例: %s\n" "$description"
        printf "       响应: %s\n" "$(head -c 300 "$out")"
    fi
}

echo "============================================================"
echo "QA Live Healthcare API 测试"
echo "BASE_URL : $BASE_URL"
echo "开始时间 : $(date '+%Y-%m-%d %H:%M:%S')"
echo "测试账号 : $TEST_USERNAME"
echo "============================================================"

HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "${BASE_URL}/actuator/health" || echo "000")
if [ "$HEALTH_STATUS" != "200" ]; then
    echo "错误: 后端未就绪(${BASE_URL}/actuator/health -> HTTP ${HEALTH_STATUS})"
    exit 2
fi

# ---------- 请求体 ----------
cat > "$TMP_DIR/doctor-login-ok.json" <<'JSON'
{"username":"dr-zhang-wei","password":"123456"}
JSON

cat > "$TMP_DIR/doctor-login-bad.json" <<'JSON'
{"username":"dr-zhang-wei","password":"wrong-password"}
JSON

cat > "$TMP_DIR/doctor-login-blank.json" <<'JSON'
{}
JSON

cat > "$TMP_DIR/patient-login-seed.json" <<'JSON'
{"username":"patient-zhao-ming","password":"123456"}
JSON

cat > "$TMP_DIR/patient-register.json" <<JSON
{
  "username": "$TEST_USERNAME",
  "password": "123456",
  "name": "接口测试",
  "birthday": "1990-01-01",
  "phone": "13800000000",
  "gender": "男"
}
JSON

cat > "$TMP_DIR/patient-register-short.json" <<JSON
{
  "username": "$SHORT_PASSWORD_USER",
  "password": "123",
  "name": "Short Password",
  "birthday": "1990-01-01"
}
JSON

cat > "$TMP_DIR/patient-register-invalid-username.json" <<JSON
{
  "username": "ab",
  "password": "123456",
  "name": "Bad Username",
  "birthday": "1990-01-01"
}
JSON

cat > "$TMP_DIR/patient-register-no-birthday.json" <<JSON
{
  "username": "$NO_BIRTHDAY_USER",
  "password": "123456",
  "name": "No Birthday"
}
JSON

cat > "$TMP_DIR/patient-login-new.json" <<JSON
{"username": "$TEST_USERNAME", "password": "123456"}
JSON

cat > "$TMP_DIR/patient-login-bad.json" <<JSON
{"username": "$TEST_USERNAME", "password": "wrong-password"}
JSON

cat > "$TMP_DIR/patient-login-unknown.json" <<JSON
{"username": "patient-nobody-${TEST_SUFFIX}", "password": "123456"}
JSON

cat > "$TMP_DIR/malformed.json" <<'JSON'
{bad json
JSON

# ---------- 医生接口 ----------
run_case "D01" "医生列表默认分页" GET "/api/v1/doctors" "" "200" \
    "data['total'] == 5 and len(data['items']) == 5 and all('password' not in item and 'passwordHash' not in item for item in data['items'])"
run_case "D02" "仅在线医生" GET "/api/v1/doctors?isActive=true" "" "200" \
    "data['total'] == 4 and all(item['isActive'] for item in data['items'])"
run_case "D03" "分页参数 page=2,size=2" GET "/api/v1/doctors?page=2&size=2" "" "200" \
    "data['page'] == 2 and data['size'] == 2 and len(data['items']) == 2 and data['total'] == 5"
run_case "D04" "页码非法 page=0" GET "/api/v1/doctors?page=0" "" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR'"
run_case "D05" "分页过大 size=101" GET "/api/v1/doctors?size=101" "" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR'"
run_case "D06" "按 ID 查询医生" GET "/api/v1/doctors/doc001" "" "200" \
    "data['id'] == 'doc001' and data['name'] == '张伟医生' and 'passwordHash' not in data"
run_case "D07" "医生不存在" GET "/api/v1/doctors/doc999" "" "404" \
    "data['error']['code'] == 'NOT_FOUND'"
run_case "D08" "按用户名查询医生" GET "/api/v1/doctors/username/dr-zhang-wei" "" "200" \
    "data['username'] == 'dr-zhang-wei'"
run_case "D09" "用户名不存在" GET "/api/v1/doctors/username/dr-nobody" "" "404" \
    "data['error']['code'] == 'NOT_FOUND'"
run_case "D10" "医生登录成功" POST "/api/v1/doctors/login" "$TMP_DIR/doctor-login-ok.json" "200" \
    "data['doctor']['id'] == 'doc001' and 'passwordHash' not in data['doctor']"
run_case "D11" "医生登录密码错误" POST "/api/v1/doctors/login" "$TMP_DIR/doctor-login-bad.json" "401" \
    "data['error']['code'] == 'UNAUTHORIZED'"
run_case "D12" "医生登录参数为空" POST "/api/v1/doctors/login" "$TMP_DIR/doctor-login-blank.json" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR'"

# ---------- 患者接口 ----------
run_case "P01" "种子患者登录" POST "/api/v1/patients/login" "$TMP_DIR/patient-login-seed.json" "200" \
    "data['patient']['id'] == 'patient001' and 'passwordHash' not in data['patient']"
run_case "P02" "患者注册成功" POST "/api/v1/patients/register" "$TMP_DIR/patient-register.json" "201" \
    "data['patient']['username'] == '$TEST_USERNAME' and 'password' not in data['patient'] and 'passwordHash' not in data['patient']"
run_case "P03" "用户名重复" POST "/api/v1/patients/register" "$TMP_DIR/patient-register.json" "409" \
    "data['error']['code'] == 'CONFLICT'"
run_case "P04" "密码长度不足" POST "/api/v1/patients/register" "$TMP_DIR/patient-register-short.json" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR' and 'password' in data['error']['details']"
run_case "P05" "用户名格式非法" POST "/api/v1/patients/register" "$TMP_DIR/patient-register-invalid-username.json" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR' and 'username' in data['error']['details']"
run_case "P06" "缺少生日" POST "/api/v1/patients/register" "$TMP_DIR/patient-register-no-birthday.json" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR' and 'birthday' in data['error']['details']"
run_case "P07" "新患者登录" POST "/api/v1/patients/login" "$TMP_DIR/patient-login-new.json" "200" \
    "data['patient']['username'] == '$TEST_USERNAME'"
run_case "P08" "患者密码错误" POST "/api/v1/patients/login" "$TMP_DIR/patient-login-bad.json" "401" \
    "data['error']['code'] == 'UNAUTHORIZED'"
run_case "P09" "患者用户名不存在" POST "/api/v1/patients/login" "$TMP_DIR/patient-login-unknown.json" "401" \
    "data['error']['code'] == 'UNAUTHORIZED'"
run_case "P10" "请求体格式非法" POST "/api/v1/patients/login" "$TMP_DIR/malformed.json" "400" \
    "data['error']['code'] == 'VALIDATION_ERROR'"

echo "============================================================"
echo "通过: $PASS    失败: $FAIL    总计: $((PASS + FAIL))"
echo "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "清理测试账号 SQL:"
echo "  DELETE FROM patient WHERE username = '$TEST_USERNAME';"
echo "============================================================"

[ "$FAIL" -eq 0 ]
