package xyz.quartzframework.data.query;

import java.lang.reflect.Method;

public class SimpleQueryParser implements QueryParser {

    private final MethodQueryParser methodQueryParser = new MethodQueryParser();

    private final QQLQueryParser qqlQueryParser = new QQLQueryParser();

    @Override
    public DynamicQueryDefinition parse(Method method) {
        if (qqlQueryParser.supports(method)) {
            return qqlQueryParser.parse(method);
        }
        return methodQueryParser.parse(method);
    }

    @Override
    public boolean supports(Method method) {
        return methodQueryParser.supports(method) || qqlQueryParser.supports(method);
    }

    @Override
    public String queryString(Method method) {
        if (qqlQueryParser.supports(method)) {
            return qqlQueryParser.queryString(method);
        }
        return methodQueryParser.queryString(method);
    }
}
