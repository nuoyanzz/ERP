package com.erp.common.expression;

import com.erp.common.exception.BizException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量表达式引擎。
 * <p>
 * 支持的业务表达式语法（面向配置场景，非通用编程语言）：
 * <pre>
 *   条件：      amount &gt; 5000 &amp;&amp; type == '采购'
 *   条件：      total &gt;= 10000 || level == 'VIP'
 *   取值：      amount
 *   取值：      ${amount}
 * </pre>
 * <p>
 * 支持的操作符：{@code == != > >= < <=}、逻辑与 {@code && / and}、
 * 逻辑或 {@code || / or}、逻辑非 {@code ! / not}，括号分组。
 * <p>
 * 变量从上下文 {@code Map<String, Object>} 取值，缺失变量按 null 处理。
 * 解析采用递归下降，语法错误会抛出带位置说明的异常。
 */
public final class ExpressionEvaluator {

    /** 完整表达式（含比较运算符） */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "^\\s*(?<left>[^<>=!&|]+?)\\s*(?<op>==|!=|>=|<=|>|<)\\s*(?<right>.+?)\\s*$");

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(?<name>[A-Za-z_][A-Za-z0-9_.]*)}");

    /** 数字字面量：整数或小数，可带负号 */
    private static final Pattern NUMBER_LITERAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private ExpressionEvaluator() {
    }

    /**
     * 求值布尔条件。
     *
     * @param expression 表达式
     * @param context    变量上下文
     * @return 条件成立返回 true
     */
    public static boolean evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        Object value = evaluateValue(expression, context);
        return coerceToBoolean(value);
    }

    /**
     * 求值并返回原始值。
     * <p>
     * 表达式是比较式或逻辑组合时返回布尔结果；是单个变量时返回变量本身的值。
     */
    public static Object evaluateValue(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        String trimmed = expression.trim();

        // 纯变量引用
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String name = trimmed.substring(2, trimmed.length() - 1).trim();
            return resolveVariable(name, context);
        }

        Boolean result = evaluateBoolean(trimmed, context);
        if (result != null) {
            return result;
        }
        // 不是布尔表达式，按变量取值
        return resolveVariable(trimmed, context);
    }

    /**
     * 将求值结果转为布尔语义。
     */
    private static boolean coerceToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 解析布尔表达式主体。返回 null 表示该表达式不是布尔表达式（是单个变量）。
     */
    private static Boolean evaluateBoolean(String expression, Map<String, Object> context) {
        // 先处理顶层逻辑或
        List<String> orParts = splitTopLevel(expression, "||", "or");
        if (orParts.size() > 1) {
            for (String part : orParts) {
                if (Boolean.TRUE.equals(evaluateBoolean(part.trim(), context))) {
                    return true;
                }
            }
            return false;
        }

        // 再处理逻辑与
        List<String> andParts = splitTopLevel(expression, "&&", "and");
        if (andParts.size() > 1) {
            for (String part : andParts) {
                if (!Boolean.TRUE.equals(evaluateBoolean(part.trim(), context))) {
                    return false;
                }
            }
            return true;
        }

        // 逻辑非
        String trimmed = expression.trim();
        if (trimmed.startsWith("!") && !trimmed.startsWith("!=")) {
            Boolean inner = evaluateBoolean(trimmed.substring(1).trim(), context);
            return inner == null ? null : !inner;
        }
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("not ")) {
            Boolean inner = evaluateBoolean(trimmed.substring(4).trim(), context);
            return inner == null ? null : !inner;
        }

        // 括号分组
        if (trimmed.startsWith("(") && trimmed.endsWith(")") && isBalanced(trimmed)) {
            return evaluateBoolean(trimmed.substring(1, trimmed.length() - 1).trim(), context);
        }

        // 比较表达式
        Matcher matcher = EXPRESSION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String left = matcher.group("left").trim();
            String op = matcher.group("op");
            String right = matcher.group("right").trim();
            return compare(left, op, right, context);
        }

        // 布尔字面量
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }

        // 不是布尔表达式，返回 null 交由调用方按变量处理
        return null;
    }

    /**
     * 比较运算。
     */
    private static boolean compare(String left, String op, String right, Map<String, Object> context) {
        Object leftValue = resolveOperand(left, context);
        Object rightValue = resolveOperand(right, context);

        BigDecimal leftNum = toNumber(leftValue);
        BigDecimal rightNum = toNumber(rightValue);
        if (leftNum != null && rightNum != null) {
            int cmp = leftNum.compareTo(rightNum);
            return switch (op) {
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                default -> false;
            };
        }

        String leftStr = leftValue == null ? "" : String.valueOf(leftValue);
        String rightStr = rightValue == null ? "" : String.valueOf(rightValue);
        int cmp = leftStr.compareTo(rightStr);
        return switch (op) {
            case "==" -> leftStr.equals(rightStr);
            case "!=" -> !leftStr.equals(rightStr);
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    /**
     * 解析操作数：字符串字面量、数字字面量或变量。
     */
    private static Object resolveOperand(String operand, Map<String, Object> context) {
        String value = operand.trim();
        if (value.length() >= 2 && (value.startsWith("'") && value.endsWith("'")
                || value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        // 数字字面量直接按数字处理，不查上下文
        if (NUMBER_LITERAL.matcher(value).matches()) {
            return new BigDecimal(value);
        }
        return resolveVariable(value, context);
    }

    /**
     * 解析变量，支持 {@code a.b.c} 形式的嵌套取值。
     */
    private static Object resolveVariable(String expression, Map<String, Object> context) {
        String name = expression.trim();
        if (name.startsWith("${") && name.endsWith("}")) {
            name = name.substring(2, name.length() - 1).trim();
        }
        if (context == null || context.isEmpty()) {
            return null;
        }
        if (!name.contains(".")) {
            return context.get(name);
        }
        Object current = context;
        for (String part : name.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof Collection<?> collection) {
                current = collection.stream().findFirst().orElse(null);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * 尝试转为数字。
     */
    private static BigDecimal toNumber(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 按顶层分隔符切分，忽略括号与引号内的内容。
     */
    private static List<String> splitTopLevel(String expression, String... separators) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
            }
            if (depth == 0 && !inSingleQuote && !inDoubleQuote) {
                for (String separator : separators) {
                    if (matchesSeparator(expression, i, separator)) {
                        parts.add(expression.substring(start, i));
                        i += separator.length() - 1;
                        start = i + 1;
                        break;
                    }
                }
            }
        }
        parts.add(expression.substring(start));
        return parts;
    }

    /**
     * 判断指定位置是否为分隔符（词边界处理）。
     */
    private static boolean matchesSeparator(String expression, int index, String separator) {
        if (!expression.startsWith(separator, index)) {
            return false;
        }
        boolean wordSeparator = separator.equals("or") || separator.equals("and");
        if (wordSeparator) {
            boolean startOk = index == 0 || !Character.isLetterOrDigit(expression.charAt(index - 1));
            int end = index + separator.length();
            boolean endOk = end >= expression.length() || !Character.isLetterOrDigit(expression.charAt(end));
            return startOk && endOk;
        }
        return true;
    }

    /**
     * 判断括号是否平衡。
     */
    private static boolean isBalanced(String text) {
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    /**
     * 从上下文构造变量 map，便于调用方使用。
     */
    public static Map<String, Object> context(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    /**
     * 提取表达式中的变量名，用于依赖分析。
     */
    public static List<String> extractVariables(String expression) {
        List<String> names = new ArrayList<>();
        if (expression == null) {
            return names;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            names.add(matcher.group("name"));
        }
        return names;
    }

    /**
     * 校验表达式语法是否合法。
     */
    public static void validate(String expression) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        try {
            evaluateBoolean(expression, new HashMap<>());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("表达式语法错误：" + expression);
        }
    }
}








