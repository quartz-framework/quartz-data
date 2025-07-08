package xyz.quartzframework.data.query;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
public class QueryCondition {

    private final AttributePath attribute;
    private final Operation operation;
    private final Object fixedValue;
    private final Integer paramIndex;
    private final String namedParameter;
    private final boolean ignoreCase;

    private final String rawCondition;
    private final String rawValue;
    @Setter
    private boolean or = false;

    public QueryCondition(
            String rawCondition,
            AttributePath attribute,
            Operation operation,
            @Nullable Object fixedValue,
            @Nullable Integer paramIndex,
            @Nullable String namedParameter,
            @Nullable String rawValue,
            boolean ignoreCase
    ) {
        this.rawCondition = rawCondition;
        this.attribute = attribute;
        this.operation = operation;
        this.fixedValue = fixedValue;
        this.paramIndex = paramIndex;
        this.namedParameter = namedParameter;
        this.rawValue = rawValue;
        this.ignoreCase = ignoreCase;
    }

    public QueryCondition(
            String rawProperty,
            String attributeName,
            Operation operation,
            @Nullable Object fixedValue,
            @Nullable Integer paramIndex,
            @Nullable String namedParameter,
            @Nullable String rawValue,
            boolean ignoreCase,
            CaseFunction caseFunction
    ) {
        this(
                rawProperty + " " + operation + " " + rawValue,
                new AttributePath(rawProperty, attributeName, caseFunction),
                operation,
                fixedValue,
                paramIndex,
                namedParameter,
                rawValue,
                ignoreCase
        );
    }

    public QueryCondition(
            String attributeName,
            Operation operation,
            Object fixedValue,
            Integer paramIndex,
            boolean ignoreCase,
            CaseFunction caseFunction
    ) {
        this(attributeName, attributeName, operation, fixedValue, paramIndex, null, null, ignoreCase, caseFunction);
    }

    public QueryCondition(
            String attributeName,
            Operation operation,
            Object fixedValue,
            boolean ignoreCase,
            CaseFunction caseFunction
    ) {
        this(attributeName, operation, fixedValue, null, ignoreCase, caseFunction);
    }

    public String getAttributeName() {
        return attribute.name();
    }

    public String getRawAttribute() {
        return attribute.raw();
    }

    public CaseFunction getCaseFunction() {
        return attribute.caseFunction();
    }

    public boolean isCaseInsensitive() {
        return ignoreCase || attribute.ignoreCase();
    }

}