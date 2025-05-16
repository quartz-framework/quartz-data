package xyz.quartzframework.data.query;

import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NamedInstance;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Injectable
@NamedInstance("methodQueryParser")
public class MethodQueryParser implements QueryParser {

    @Override
    public boolean supports(Method method) {
        return method.getAnnotations().length == 0 && method.getName().matches("^(find|count|exists).*");
    }

    @Override
    public String queryString(Method method) {
        return method.getName();
    }

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

    @Override
    public DynamicQueryDefinition parse(Method method) {
        val name = queryString(method);
        QueryAction action = extractAction(name);
        String stripped = stripPrefix(name, action);
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