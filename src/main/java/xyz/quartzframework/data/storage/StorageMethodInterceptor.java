package xyz.quartzframework.data.storage;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;
import xyz.quartzframework.data.query.DynamicQueryDefinition;
import xyz.quartzframework.data.query.QuartzQuery;
import xyz.quartzframework.data.query.QueryExecutor;
import xyz.quartzframework.data.query.QueryParser;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class StorageMethodInterceptor<E> implements MethodInterceptor {

    private final QueryExecutor<E> executor;
    private final Class<E> entityType;

    public StorageMethodInterceptor(QueryExecutor<E> executor, Class<E> entityType) {
        this.executor = executor;
        this.entityType = entityType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getDeclaringClass().equals(Object.class)
                || method.getName().equals("toString")
                || method.getName().equals("equals")
                || method.getName().equals("hashCode")) {
            return invocation.proceed();
        }
        if (method.isDefault()) {
            return invocation.proceed();
        }
        QuartzQuery annotation = method.getAnnotation(QuartzQuery.class);
        if (!isDynamicMethod(method)) return invocation.proceed();
        DynamicQueryDefinition query = annotation == null ? QueryParser.parse(method.getName()) : QueryParser.parseFriendly(annotation.value());
        String queryString = annotation != null ? annotation.value() : method.getName();
        validateReturnType(method, query);
        Object[] args = invocation.getArguments();
        long dynamicConditions = query
                .conditions()
                .stream()
                .filter(c -> c.fixedValue() == null)
                .count();
        if (args.length < dynamicConditions) {
            throw new IllegalStateException("Expected " + dynamicConditions + " arguments for query '" + queryString + "', but got " + args.length);
        }
        List<E> results = Optional.ofNullable(executor.execute(query, args)).orElse(Collections.emptyList());
        Class<?> returnType = method.getReturnType();
        return switch (query.action()) {
            case FIND -> handleFind(returnType, results, args, queryString);
            case EXISTS -> handleExists(returnType, results, queryString);
            case COUNT -> handleCount(returnType, results, queryString);
        };
    }

    private Object handleExists(Class<?> returnType, List<E> results, String methodName) {
        if (returnType == boolean.class || returnType == Boolean.class) {
            return !results.isEmpty();
        }
        throw new UnsupportedOperationException("EXISTS methods must return boolean: " + methodName);
    }

    private Object handleCount(Class<?> returnType, List<E> results, String methodName) {
        if (returnType == long.class || returnType == Long.class || Number.class.isAssignableFrom(returnType)) {
            return (long) results.size();
        }
        throw new UnsupportedOperationException("COUNT methods must return long or Number: " + methodName);
    }

    private Object handleFind(Class<?> returnType, List<E> results, Object[] args, String name) {
        if (Set.class.isAssignableFrom(returnType)) {
            return new HashSet<>(results);
        }
        if (Stream.class.isAssignableFrom(returnType)) {
            return results.stream();
        }
        if (List.class.isAssignableFrom(returnType)) {
            return results;
        }
        if (Optional.class.isAssignableFrom(returnType)) {
            return results.stream().findFirst();
        }
        if (Page.class.isAssignableFrom(returnType)) {
            Pagination pagination = Stream.of(args)
                    .filter(arg -> arg instanceof Pagination)
                    .map(Pagination.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Pagination required for paged method"));
            return Page.fromList(results, pagination);
        }
        if (entityType.isAssignableFrom(returnType)) {
            return results.stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No result found for: " + name));
        }
        throw new UnsupportedOperationException("Unsupported return type in FIND: " + returnType.getName());
    }

    private boolean isDynamicMethod(Method method) {
        if (method.isAnnotationPresent(QuartzQuery.class)) return true;
        String name = method.getName();
        return name.startsWith("find") ||
                name.startsWith("count") ||
                name.startsWith("exists");
    }

    private void validateReturnType(Method method, DynamicQueryDefinition query) {
        Class<?> returnType = method.getReturnType();
        String methodName = method.getName();
        switch (query.action()) {
            case FIND -> {
                if (!isSupportedFindReturnType(returnType)) {
                    throw new UnsupportedOperationException("FIND return type not supported: " + returnType.getName());
                }
            }
            case EXISTS -> {
                if (!(returnType == boolean.class || returnType == Boolean.class)) {
                    throw new UnsupportedOperationException("EXISTS must return boolean: " + methodName);
                }
            }
            case COUNT -> {
                if (!(returnType == long.class || returnType == Long.class || Number.class.isAssignableFrom(returnType))) {
                    throw new UnsupportedOperationException("COUNT must return numeric type: " + methodName);
                }
            }
        }
    }

    private boolean isSupportedFindReturnType(Class<?> returnType) {
        return List.class.isAssignableFrom(returnType) ||
                Set.class.isAssignableFrom(returnType) ||
                Stream.class.isAssignableFrom(returnType) ||
                Optional.class.isAssignableFrom(returnType) ||
                Page.class.isAssignableFrom(returnType) ||
                entityType.isAssignableFrom(returnType);
    }
}
