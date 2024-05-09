package com.lpzahd.cglib;

import java.util.List;

/**
 * Class Name: Filter
 * Package: com.lpzahd.ease.framework.cglib
 * Description: 根据特定条件接受或拒绝对象的过滤器接口
 * @author lpzahd
 * Create DateTime: 2023/8/30 10:08
 * Version: 1.0.0
 */
public interface Filter<S, T> {

    boolean accept(S sourceValue, String sourceName, T targetValue, String targetName);

    default Filter<S, T> and(Filter<? super S, ? super T> other) {
        return (sourceValue, sourceName, targetValue, targetName) ->
                this.accept(sourceValue, sourceName, targetValue, targetName) &&
                        other.accept(sourceValue, sourceName, targetValue, targetName);
    }

    default Filter<S, T> negate() {
        return (sourceValue, sourceName, targetValue, targetName) ->
                !this.accept(sourceValue, sourceName, targetValue, targetName);
    }

    default Filter<S, T> or(Filter<? super S, ? super T> other) {
        return (sourceValue, sourceName, targetValue, targetName) ->
                this.accept(sourceValue, sourceName, targetValue, targetName) ||
                        other.accept(sourceValue, sourceName, targetValue, targetName);
    }

    /**
     * 基于属性名称列表来忽略属性的过滤器实现。
     */
    class IgnorePropertiesFilter implements Filter<Object, Object> {
        private final List<String> properties;

        public IgnorePropertiesFilter(List<String> properties) {
            this.properties = properties;
        }

        @Override
        public boolean accept(Object sourceValue, String sourceName, Object targetValue, String targetName) {
            return !properties.contains(sourceName);
        }
    }

    /**
     * 覆盖方式
     */
    Filter<Object, Object> FILTER_OVERRIDE = (sourceValue, sourceName, targetValue, targetName) -> true;

    /**
     * 如果目标value为null
     */
    Filter<Object, Object> FILTER_IF_ABSENT = (sourceValue, sourceName, targetValue, targetName) -> targetValue == null;

    /**
     * 忽略null
     */
    Filter<Object, Object> FILTER_IGNORE_NULL = (sourceValue, sourceName, targetValue, targetName) -> sourceValue != null;

    static Filter<Object, Object> ignorePropertiesFilter(List<String> properties) {
        return new IgnorePropertiesFilter(properties);
    }
}
