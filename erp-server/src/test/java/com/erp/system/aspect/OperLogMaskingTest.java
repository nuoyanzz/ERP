package com.erp.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 操作日志敏感字段脱敏的单元测试。
 * <p>
 * 改密码、重置密码等接口的请求体里带着明文口令，
 * 若原样写入 sys_oper_log 表，口令就以明文形式留在了数据库里。
 */
class OperLogMaskingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 与切面内部一致的脱敏逻辑，此处独立复刻以验证规则本身。
     */
    private String mask(Map<String, Object> params) {
        try {
            var node = objectMapper.valueToTree(params);
            maskNode(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private void maskNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (isSensitive(entry.getKey()) && entry.getValue().isValueNode()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(entry.getKey(), "******");
                } else {
                    maskNode(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            for (var child : node) {
                maskNode(child);
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName.toLowerCase().replace("_", "").replace("-", "");
        return java.util.Set.of("password", "oldpassword", "newpassword", "confirmpassword",
                "passwd", "pwd", "secret", "token", "accesstoken", "refreshtoken")
                .contains(normalized);
    }

    @Test
    @DisplayName("口令字段被脱敏为掩码")
    void passwordIsMasked() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "MySecret@2026");

        String result = mask(params);

        assertFalse(result.contains("MySecret@2026"), "明文口令不应出现在日志中: " + result);
        assertTrue(result.contains("******"));
        assertTrue(result.contains("admin"), "非敏感字段应保留");
    }

    @Test
    @DisplayName("下划线与驼峰写法都能识别")
    void underscoreAndCamelCaseBothMasked() {
        Map<String, Object> params = new HashMap<>();
        params.put("old_password", "old-one");
        params.put("newPassword", "new-one");

        String result = mask(params);

        assertFalse(result.contains("old-one"));
        assertFalse(result.contains("new-one"));
    }

    @Test
    @DisplayName("令牌类字段同样脱敏")
    void tokenIsMasked() {
        Map<String, Object> params = new HashMap<>();
        params.put("accessToken", "eyJhbGciOiJIUzI1NiJ9.xxx");
        params.put("refresh_token", "eyJhbGciOiJIUzI1NiJ9.yyy");

        String result = mask(params);

        assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    @DisplayName("字段名包含敏感词但不相同的不误伤")
    void similarNamesNotMasked() {
        Map<String, Object> params = new HashMap<>();
        params.put("passwordPolicy", "8位含字母数字");
        params.put("tokenCount", 3);

        String result = mask(params);

        assertTrue(result.contains("8位含字母数字"));
        assertTrue(result.contains("3"));
    }

    @Test
    @DisplayName("业务字段不受影响")
    void businessFieldsUntouched() {
        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", "CG-202610-0001");
        params.put("amount", 141.25);

        String result = mask(params);

        assertTrue(result.contains("CG-202610-0001"));
        assertTrue(result.contains("141.25"));
    }
}
