package xyz.quartzframework.data.util;

import lombok.experimental.UtilityClass;
import xyz.quartzframework.data.query.DynamicQueryDefinition;
import xyz.quartzframework.data.query.ParameterBindingException;
import xyz.quartzframework.data.query.QueryCondition;
import xyz.quartzframework.data.query.QueryParameter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public final class ParameterBindingUtil {

    public void validateNamedParameters(Method method, DynamicQueryDefinition definition) {
        Set<String> declared = new HashSet<>();
        for (Parameter parameter : method.getParameters()) {
            QueryParameter qp = parameter.getAnnotation(QueryParameter.class);
            if (qp != null) {
                declared.add(qp.value());
            }
        }

        for (QueryCondition queryCondition : definition.queryConditions()) {
            String named = queryCondition.getNamedParameter();
            if (named != null && !declared.contains(named)) {
                throw new ParameterBindingException("Missing @QueryParameter(\"" + named + "\") for method: " + method.getName());
            }
        }
    }

    public Object findNamedParameter(Method method, String name, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            QueryParameter qp = parameters[i].getAnnotation(QueryParameter.class);
            if (qp != null && qp.value().equals(name)) {
                return args[i];
            }
        }
        throw new ParameterBindingException("Missing required @QueryParameter(\"" + name + "\") binding");
    }
}