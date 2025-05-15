package xyz.quartzframework.data.query;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class QueryParser {

    private static final Map<String, Operation> suffixAlias = Map.ofEntries(
            Map.entry("After", Operation.GREATER_THAN),
            Map.entry("Before", Operation.LESS_THAN),
            Map.entry("GreaterThan", Operation.GREATER_THAN),
            Map.entry("LessThan", Operation.LESS_THAN),
            Map.entry("GreaterThanOrEqual", Operation.GREATER_THAN_OR_EQUAL),
            Map.entry("LessThanOrEqual", Operation.LESS_THAN_OR_EQUAL),
            Map.entry("Not", Operation.NOT_EQUAL),
            Map.entry("Like", Operation.LIKE),
            Map.entry("NotLike", Operation.NOT_LIKE),
            Map.entry("In", Operation.IN),
            Map.entry("NotIn", Operation.NOT_IN),
            Map.entry("IsNull", Operation.IS_NULL),
            Map.entry("IsNotNull", Operation.IS_NOT_NULL),
            Map.entry("True", Operation.EQUAL),
            Map.entry("False", Operation.EQUAL)
    );

    public DynamicQueryDefinition parse(String methodName) {
        QueryAction action = extractAction(methodName);
        String stripped = stripPrefix(methodName, action);
        List<Condition> conditions = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
        Integer limit = null;
        String conditionPart = stripped;
        if (stripped.contains("OrderBy")) {
            String[] split = stripped.split("OrderBy", 2);
            conditionPart = split[0];
            orders = parseOrderPart(split[1]);
        }
        if (conditionPart.startsWith("Top")) {
            Matcher m = Pattern.compile("Top(\\d+)(.*)").matcher(conditionPart);
            if (m.matches()) {
                limit = Integer.parseInt(m.group(1));
                conditionPart = m.group(2);
            }
        } else if (conditionPart.startsWith("First")) {
            limit = 1;
            conditionPart = conditionPart.substring(5);
        }
        if (conditionPart.startsWith("By")) {
            conditionPart = conditionPart.substring(2);
        }
        if (!conditionPart.isEmpty()) {
            conditions = parseConditions(conditionPart);
        }
        return new DynamicQueryDefinition(action, conditions, orders, limit);
    }

    public DynamicQueryDefinition parseFriendly(String input) {
        String lower = input.toLowerCase(Locale.ROOT).trim();
        QueryAction action;
        if (lower.startsWith("find")) action = QueryAction.FIND;
        else if (lower.startsWith("count")) action = QueryAction.COUNT;
        else if (lower.startsWith("exists")) action = QueryAction.EXISTS;
        else throw new IllegalArgumentException("Unknown query action: " + input);
        String query = input.substring(action.name().length()).trim();
        Integer limit = null;
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
                    "(\\w+)\\s*(not like|not in|is not null|is null|>=|<=|!=|<>|=|>|<|like|in)\\s*(\\?\\d*|true|false|null|'[^']*')?",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher m = condPattern.matcher(conditionPart);
            int positionalParamCounter = 0;
            while (m.find()) {
                String field = m.group(1);
                String operator = m.group(2).toLowerCase();
                String value = m.group(3) != null ? m.group(3).trim() : null;

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
                if (value != null) {
                    if (value.startsWith("?")) {
                        if (value.length() == 1) {
                            paramIndex = positionalParamCounter++;
                        } else {
                            paramIndex = Integer.parseInt(value.substring(1)) - 1;
                        }
                    } else if (value.equalsIgnoreCase("true")) {
                        fixedValue = Boolean.TRUE;
                    } else if (value.equalsIgnoreCase("false")) {
                        fixedValue = Boolean.FALSE;
                    } else if (value.equalsIgnoreCase("null")) {
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        fixedValue = value.substring(1, value.length() - 1);
                    } else {
                        throw new IllegalArgumentException("Unsupported value literal: " + value);
                    }
                }
                conditions.add(new Condition(field, op, fixedValue, paramIndex));
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
        return new DynamicQueryDefinition(action, conditions, orders, limit);
    }

    private QueryAction extractAction(String methodName) {
        if (methodName.startsWith("find")) return QueryAction.FIND;
        if (methodName.startsWith("count")) return QueryAction.COUNT;
        if (methodName.startsWith("exists")) return QueryAction.EXISTS;
        throw new IllegalArgumentException("Unknown query action: " + methodName);
    }

    private String stripPrefix(String methodName, QueryAction action) {
        return methodName.substring(action.name().length()).replaceFirst("^By", "By");
    }

    private List<Condition> parseConditions(String part) {
        List<Condition> conditions = new ArrayList<>();
        String[] tokens = part.split("And");
        for (String token : tokens) {
            Condition condition = parseConditionToken(token);
            conditions.add(condition);
        }
        return conditions;
    }

    private Condition parseConditionToken(String token) {
        for (String suffix : suffixAlias
                .keySet()
                .stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList()) {
            if (token.endsWith(suffix)) {
                Operation op = suffixAlias.get(suffix);
                String prop = token.substring(0, token.length() - suffix.length());
                Object fixedValue = switch (suffix) {
                    case "True" -> true;
                    case "False" -> false;
                    case "IsNull", "IsNotNull" -> Boolean.TRUE;
                    default -> null;
                };
                return new Condition(lowerFirst(prop), op, fixedValue);
            }
        }
        return new Condition(lowerFirst(token), Operation.EQUAL, null);
    }

    private List<Order> parseOrderPart(String orderPart) {
        List<Order> orders = new ArrayList<>();
        Pattern pattern = Pattern.compile("([A-Z][a-zA-Z0-9]*)(Asc|Desc)$");
        Matcher matcher = pattern.matcher(orderPart);
        while (matcher.find()) {
            String prop = matcher.group(1);
            String direction = matcher.group(2);
            orders.add(new Order(lowerFirst(prop), "Desc".equalsIgnoreCase(direction)));
        }
        return orders;
    }

    private String lowerFirst(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
