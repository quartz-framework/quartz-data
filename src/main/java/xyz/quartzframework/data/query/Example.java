package xyz.quartzframework.data.query;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.Objects;

@RequiredArgsConstructor
public class Example<T> {

    private final T probe;

    public boolean matches(T candidate) {
        if (candidate == null) return false;
        if (!probe.getClass().isAssignableFrom(candidate.getClass())) return false;
        for (Field field : probe.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object probeValue = field.get(probe);
                if (probeValue == null) continue;
                Object candidateValue = field.get(candidate);
                if (!Objects.equals(probeValue, candidateValue)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field " + field.getName(), e);
            }
        }
        return true;
    }

    public static <T> Example<T> of(T probe) {
        return new Example<>(probe);
    }
}