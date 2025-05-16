package xyz.quartzframework.data.query;

import lombok.RequiredArgsConstructor;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;

import java.lang.reflect.Method;

@RequiredArgsConstructor
public class CompositeQueryParser implements QueryParser {

    private final PluginBeanFactory beanFactory;

    @Override
    public DynamicQueryDefinition parse(Method method) {
        for (QueryParser parser : beanFactory.getBeansOfType(QueryParser.class).values()) {
            if (parser.getClass().equals(CompositeQueryParser.class)) {
                continue;
            }
            if (parser.supports(method)) {
                return parser.parse(method);
            }
        }
        throw new IllegalStateException("No QueryParser could handle method: " + method.getName());
    }

    @Override
    public boolean supports(Method method) {
        return true;
    }

    @Override
    public String queryString(Method method) {
        for (QueryParser parser : beanFactory.getBeansOfType(QueryParser.class).values()) {
            if (parser.supports(method)) {
                return parser.queryString(method);
            }
        }
        throw new IllegalStateException("No QueryParser could handle method: " + method.getName());
    }
}