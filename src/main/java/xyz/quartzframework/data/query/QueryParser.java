package xyz.quartzframework.data.query;

import java.lang.reflect.Method;

public interface QueryParser {

    DynamicQueryDefinition parse(Method method);

    boolean supports(Method method);

    String queryString(Method method);

}