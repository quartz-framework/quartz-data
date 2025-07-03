package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;
import xyz.quartzframework.data.util.ParameterBindingUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Injectable
@NamedInstance("qqlQueryParser")
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
        if (annotation == null) {
            return null;
        }
        return annotation.value();
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
                String namedParameter = null;
                if (value != null) {
                    if (value.startsWith("?")) {
                        if (value.length() == 1) {
                            paramIndex = positionalParamCounter++;
                        } else if (value.startsWith(":")) {
                            namedParameter = value.substring(1);
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
                conditions.add(new Condition(field, op, fixedValue, paramIndex, namedParameter));
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
}