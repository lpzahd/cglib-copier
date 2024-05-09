package com.lpzahd.cglib;

import com.lpzahd.cglib.selector.StandardBeanPropertySelector;
import lombok.experimental.UtilityClass;
import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;
import net.sf.cglib.core.Converter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class Name: Cglib
 * Package: com.lpzahd.ease.framework.cglib
 * Description: cglib拷贝工具
 * @author lpzahd
 * Create DateTime: 2023/8/31 9:26
 * Version: 1.0.0
 */
@UtilityClass
public class Cglib {

    public static class Property<T> {
        private final String name;
        private final Class<T> type;
        private final T value;

        public Property(String name, Class<T> type, T value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

    public static class CopierBuilder {

        private static final Map<Integer, Copier> sCacheCopiers = new ConcurrentHashMap<>();

        private final Class<?> source;
        private final Class<?> target;
        private boolean useFilter = false;
        private boolean useConverter = false;
        private BeanPropertySelector selector = Cglib.selector;

        private final Map<String, String> mapper = new HashMap<>();

        private CopierBuilder(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        public CopierBuilder filter(boolean useFilter) {
            this.useFilter = useFilter;
            return this;
        }

        public CopierBuilder converter(boolean useConverter) {
            this.useConverter = useConverter;
            return this;
        }

        public CopierBuilder mapper(Map<String, String> mapper) {
            this.mapper.putAll(mapper);
            return this;
        }

        public CopierBuilder append(String sourceAttr, String targetAttr) {
            mapper.put(targetAttr, sourceAttr);
            return this;
        }

        public CopierBuilder selector(BeanPropertySelector selector) {
            this.selector = selector;
            return this;
        }

        public Copier build() {
            int keyCode = generateKey();
            Copier copier = sCacheCopiers.get(keyCode);
            if (copier == null) {
                copier = Copier.create(source, target, useFilter, useConverter, mapper, selector);
                sCacheCopiers.put(keyCode, copier);
            }
            return copier;
        }

        private int generateKey() {
            return ((1213 * 340057 + source.hashCode()) * 340057 + target.hashCode()) * 340057 + Boolean.hashCode(useFilter) * 340057 + Boolean.hashCode(useConverter);
        }
    }

    private static final Converter SMART_CONVERTER = new SmartConvert();

    private static BeanPropertySelector selector = new StandardBeanPropertySelector();

    /**
     * MethodName: setGlobalBeanPropertySelector
     * Description: 设置全局默认BeanPropertySelector
     * @author lpzahd
     * Create DateTime: 2023/9/9 22:59
     * Version: 1.0
     */
    public static void setGlobalBeanPropertySelector(BeanPropertySelector selector) {
        Cglib.selector = selector;
    }

    public static <S, T> T copyByClass(S source, Class<T> clz) {
        return copy(source, newInstance(clz), null, null);
    }

    public static <S, T> T copyByClass(S source, Class<T> clz, Filter<Object, Object> filter, Converter converter) {
        return copy(source, newInstance(clz), filter, converter);
    }

    public static <S, T> List<T> copyListByClass(Iterable<S> sources, Class<T> target) {
        List<T> targets = new ArrayList<>();
        for (S source : sources) {
            targets.add(copy(source, newInstance(target)));
        }
        return targets;
    }

    public static <S, T> List<T> copyListByClass(Iterable<S> sources, Class<T> target, Filter<Object, Object> filter, Converter converter, Map<String, String> mapper) {
        List<T> targets = new ArrayList<>();
        for (S source : sources) {
            targets.add(copy(source, newInstance(target), filter, converter, mapper));
        }
        return targets;
    }

    public static <S, T> T copy(S source, T target) {
        builder(source.getClass(), target.getClass())
                .build()
                .copy(source, target, null, null);
        return target;
    }

    public static <S, T> T copy(S source, T target, Filter<Object, Object> filter, Converter converter) {
        builder(source.getClass(), target.getClass())
                .filter(filter != null)
                .converter(converter != null)
                .build()
                .copy(source, target, filter, converter);
        return target;
    }

    public static <S, T> T copy(S source, T target, Filter<Object, Object> filter, Converter converter, Map<String, String> mapper) {
        builder(source.getClass(), target.getClass())
                .filter(filter != null)
                .converter(converter != null)
                .mapper(mapper)
                .build()
                .copy(source, target, filter, converter);
        return target;
    }

    public static <T> T copyIgnoreProperties(Object source, T target, List<String> properties) {
        builder(source.getClass(), target.getClass())
                .filter(true)
                .build()
                .copy(source, target, Filter.ignorePropertiesFilter(properties), null);
        return target;
    }

    public static <T> T copyIgnoreNull(Object source, T target) {
        builder(source.getClass(), target.getClass())
                .filter(true)
                .build()
                .copy(source, target, Filter.FILTER_IGNORE_NULL, null);
        return target;
    }

    public static <S, T> T copyIgnoreNull(S source, T target, Converter converter) {
        builder(source.getClass(), target.getClass())
                .filter(true)
                .converter(converter != null)
                .build()
                .copy(source, target, Filter.FILTER_IGNORE_NULL, converter);
        return target;
    }

    public static <T> T copyConvert(Object source, T target, Converter converter) {
        builder(source.getClass(), target.getClass())
                .converter(true)
                .build()
                .copy(source, target, null, converter);
        return target;
    }

    public static <T> T setValue(T obj, Property<?>... properties) {
        BeanMap beanMap = BeanMap.create(obj);
        for (Property<?> property : properties) {
            Class<?> propertyType = beanMap.getPropertyType(property.name);
            if (property.type == propertyType) {
                beanMap.put(property.name, property.value);
            }
        }
        return obj;
    }

    public static <T> T addProperties(Class<T> superClass, T source, Property<?>... properties) {
        BeanGenerator generator = new BeanGenerator();
        generator.setSuperclass(superClass);
        for (Property<?> property : properties) {
            generator.addProperty(property.name, property.type);
        }
        @SuppressWarnings("unchecked")
        T child = (T) generator.create();
        T proxySource = copy(source, child);
        BeanMap beanMap = BeanMap.create(proxySource);
        for (Property<?> property : properties) {
            beanMap.put(property.name, property.value);
        }
        return proxySource;
    }

    public static <T> T addProperties(T source, Property<?>... properties) {
        @SuppressWarnings("unchecked")
        Class<T> superClass = (Class<T>) source.getClass();
        return addProperties(superClass, source, properties);
    }

    public static <T> T newInstance(Class<T> clz) {
        try {
            return clz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException ignore) {
            throw new AssertionError("当前对象缺少无参构造");
        } catch (Exception e) {
            throw new RuntimeException("创建新实例时出现异常", e);
        }
    }

    public static <S, V> V smartCopyByClass(S source, Class<V> clz) {
        return smartCopy(source, newInstance(clz));
    }

    public static <S, V> List<V> smartCopyListByClass(Collection<S> source, Class<V> clz) {
        return smartCopyList(source, clz);
    }

    public static <S, T> List<T> smartCopyList(Iterable<S> sources, Class<T> target) {
        List<T> targets = new ArrayList<>();
        for (S source : sources) {
            targets.add(smartCopy(source, newInstance(target)));
        }
        return targets;
    }

    public static <S, T> T smartCopy(S source, T target) {
        return copyIgnoreNull(source, target, SMART_CONVERTER);
    }

    public static CopierBuilder builder(Class<?> source, Class<?> target) {
        return new CopierBuilder(source, target);
    }




}
