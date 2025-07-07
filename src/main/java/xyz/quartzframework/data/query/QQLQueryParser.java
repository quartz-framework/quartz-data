package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.data.util.ParameterBindingUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQLQueryParser implements QueryParser {

    @Override
    public boolean supports(Method method) {
        val a = method.getAnnotation(Query.class);
        return a != null && isInPattern(a);
    }

    private boolean isInPattern(Query query) {
        return query.value().matches("^(find|count|exists).*");
    }

    @Override
    public String queryString(Method method) {
        val annotation = method.getAnnotation(Query.class);
        return annotation != null ? annotation.value() : null;
    }

    @Override
    public DynamicQueryDefinition parse(Method method) {
        val name = queryString(method);
        String lower = name.toLowerCase(Locale.ROOT).trim();

        QueryAction action;
        if (lower.startsWith("find")) action = QueryAction.FIND;
        else if (lower.startsWith("count")) action = QueryAction.COUNT;
        else if (lower.startsWith("exists")) action = QueryAction.EXISTS;
        else throw new IllegalArgumentException("Unknown query action: " + name);

        String query = name.substring(action.name().length()).trim();
        boolean distinct = false;
        Integer limit = null;

        if (query.toLowerCase().startsWith("distinct")) {
            distinct = true;
            query = query.substring("distinct".length()).trim();
        }

        Matcher topMatch = Pattern.compile("top\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(query);
        if (topMatch.find()) {
            limit = Integer.parseInt(topMatch.group(1));
            query = query.replaceFirst("(?i)top\\s+\\d+", "").trim();
        }

        List<Condition> conditions = new ArrayList<>();
        List<Order> orders = new ArrayList<>();

        String[] parts = query.split("(?i)order\\s+by", 2);
        String conditionPart = parts[0].replaceFirst("(?i)^where", "").trim();
        String orderPart = parts.length > 1 ? parts[1].trim() : "";

        if (!conditionPart.isEmpty()) {
            Pattern condPattern = Pattern.compile(
                    "(lower\\(\\w+\\)|upper\\(\\w+\\)|\\w+)\\s*" +
                            "(not like|not in|is not null|is null|>=|<=|!=|<>|=|>|<|like|in)\\s*" +
                            "(lower\\([^)]*\\)|upper\\([^)]*\\)|:\\w+|\\?\\d*|\\?|true|false|null|'[^']*')?",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher m = condPattern.matcher(conditionPart);
            int positionalParamCounter = 0;

            while (m.find()) {
                String rawField = m.group(1);
                String operator = m.group(2).toLowerCase();
                String rawValue = m.group(3) != null ? m.group(3).trim() : null;

                boolean ignoreCase = false;
                String field = extractInner(rawField);
                String value = rawValue != null ? extractInner(rawValue) : null;

                String fieldFunc = extractCaseFunction(rawField);
                String valueFunc = extractCaseFunction(rawValue);

                if ((fieldFunc != null || valueFunc != null) &&
                        Objects.equals(fieldFunc, valueFunc)) {
                    ignoreCase = true;
                }

                Operation op = switch (operator) {
                    case "=", "==" -> Operation.EQUAL;
                    case "!=", "<>" -> Operation.NOT_EQUAL;
                    case ">" -> Operation.GREATER_THAN;
                    case ">=" -> Operation.GREATER_THAN_OR_EQUAL;
                    case "<" -> Operation.LESS_THAN;
                    case "<=" -> Operation.LESS_THAN_OR_EQUAL;
                    case "like" -> Operation.LIKE;
                    case "not like" -> Operation.NOT_LIKE;
                    case "in" -> Operation.IN;
                    case "not in" -> Operation.NOT_IN;
                    case "is null" -> Operation.IS_NULL;
                    case "is not null" -> Operation.IS_NOT_NULL;
                    default -> throw new IllegalArgumentException("Unknown operator: " + operator);
                };

                Integer paramIndex = null;
                Object fixedValue = null;
                String namedParameter = null;

                if (rawValue != null) {
                    String innerRaw = extractInner(rawValue);
                    if (innerRaw.startsWith("?")) {
                        if (innerRaw.length() == 1) {
                            paramIndex = positionalParamCounter++;
                        } else {
                            paramIndex = Integer.parseInt(innerRaw.substring(1)) - 1;
                        }
                    } else if (innerRaw.startsWith(":")) {
                        namedParameter = innerRaw.substring(1);
                    } else if (rawValue.equalsIgnoreCase("true")) {
                        fixedValue = Boolean.TRUE;
                    } else if (rawValue.equalsIgnoreCase("false")) {
                        fixedValue = Boolean.FALSE;
                    } else if (rawValue.equalsIgnoreCase("null")) {

                    } else if (rawValue.startsWith("'") && rawValue.endsWith("'")) {
                        fixedValue = rawValue.substring(1, rawValue.length() - 1);
                    } else {
                        throw new IllegalArgumentException("Unsupported value literal: " + rawValue);
                    }
                }
                conditions.add(new Condition(field, op, fixedValue, paramIndex, namedParameter, ignoreCase));
            }
        }

        if (!orderPart.isEmpty()) {
            String[] tokens = orderPart.split(",");
            for (String token : tokens) {
                String[] orderTokens = token.trim().split("\\s+");
                String prop = orderTokens[0];
                boolean desc = orderTokens.length > 1 && orderTokens[1].equalsIgnoreCase("desc");
                orders.add(new Order(prop, desc));
            }
        }

        val def = new DynamicQueryDefinition(method, action, conditions, orders, limit, distinct, false, null);
        ParameterBindingUtil.validateNamedParameters(method, def);
        return def;
    }

    private String extractCaseFunction(String expr) {
        if (expr == null) return null;
        Matcher m = Pattern.compile("(?i)(lower|upper)\\(.*\\)").matcher(expr);
        return m.matches() ? m.group(1).toLowerCase() : null;
    }

    private String extractInner(String expr) {
        if (expr == null) return null;
        Matcher m = Pattern.compile("(?i)(?:lower|upper)\\((.+)\\)").matcher(expr);
        return m.matches() ? m.group(1).trim() : expr.trim();
    }
}