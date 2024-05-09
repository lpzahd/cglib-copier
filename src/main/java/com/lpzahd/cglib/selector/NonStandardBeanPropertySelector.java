package com.lpzahd.cglib.selector;

import com.lpzahd.cglib.BeanPropertySelector;
import net.sf.cglib.core.CodeGenerationException;
import net.sf.cglib.core.ReflectUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class Name: NonStandardBeanPropertySelector
 * Package: com.lpzahd.ease.framework.cglib.selector
 * Description: 非标准 bean 类的属性选择器，例如由 Lombok 注解生成的类
 * @author lpzahd
 * Create DateTime: 2023/9/9 12:02
 * Version: 1.0
 */
public class NonStandardBeanPropertySelector implements BeanPropertySelector {

    /**
     * set方法前缀
     */
    private static final String SET_PREFIX = "set";

    // 使用 ConcurrentHashMap 来存储已处理过的类，确保线程安全
    private final Map<Class<?>, PropertyDescriptor[]> classToGettersMap = new ConcurrentHashMap<>();

    // 使用 ConcurrentHashMap 来存储已处理过的类，确保线程安全
    private final Map<Class<?>, PropertyDescriptor[]> classToSettersMap = new ConcurrentHashMap<>();


    private static final PropertyDescriptor[] NONE = new PropertyDescriptor[0];

    @Override
    public PropertyDescriptor[] selectGetters(Class<?> clazz) {
        // 使用 computeIfAbsent 方法，确保在解析同一个类时只有一个线程在处理
        return classToGettersMap.computeIfAbsent(clazz, this::parseGetter);
    }

    @Override
    public PropertyDescriptor[] selectSetters(Class<?> clazz) {
        // 使用 computeIfAbsent 方法，确保在解析同一个类时只有一个线程在处理
        return classToSettersMap.computeIfAbsent(clazz, this::parseSetter);
    }

    private PropertyDescriptor[] parseGetter(Class<?> clazz) {
        if(clazz == Object.class) {
            return NONE;
        }

        PropertyDescriptor[] beanGetters = ReflectUtils.getBeanGetters(clazz);
        if(beanGetters.length == 0) {
            return NONE;
        }

        List<PropertyDescriptor> setters = new ArrayList<>();
        Class<?> currClazz = clazz;
        try {
            // 迭代获取所有父类的方法，直到 Object 类
            while (currClazz != null && currClazz != Object.class) {
                // 获取当前类的所有公开方法
                Method[] methods = currClazz.getDeclaredMethods();

                // 遍历方法，检查是否有非标准的 setter 方法
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.startsWith(SET_PREFIX) && method.getParameterCount() == 1) {
                        Class<?> methodReturnType = method.getReturnType();
                        if(methodReturnType == void.class || methodReturnType == currClazz) {
                            // 如果方法以 "set" 开头，只有一个参数，并且返回类型是 void或者本类对象，则认为是 setter 方法
                            String propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                            PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, null, method);
                            setters.add(descriptor);
                        }
                    }
                }

                // 获取父类并继续迭代
                currClazz = currClazz.getSuperclass();
            }
        } catch (Exception e) {
            // 处理异常，根据需要进行日志记录或其他操作
            throw new CodeGenerationException(e);
        }

        if(!setters.isEmpty()) {
            try {
                for (PropertyDescriptor getter : beanGetters) {
                    PropertyDescriptor propertyDescriptor = setters.stream().filter(e ->
                            Objects.equals(e.getName(), getter.getName())
                    ).findAny().orElse(null);
                    if(propertyDescriptor != null) {
                        getter.setWriteMethod(propertyDescriptor.getWriteMethod());
                    }

                }
            } catch (IntrospectionException e) {
                // 处理异常，根据需要进行日志记录或其他操作
                throw new CodeGenerationException(e);
            }

        }

        return beanGetters;
    }

    private PropertyDescriptor[] parseSetter(Class<?> clazz) {
        if(clazz == Object.class) {
            return NONE;
        }


        List<PropertyDescriptor> setters = new ArrayList<>();
        Class<?> currClazz = clazz;
        try {
            // 迭代获取所有父类的方法，直到 Object 类
            while (currClazz != null && currClazz != Object.class) {
                // 获取当前类的所有公开方法
                Method[] methods = currClazz.getDeclaredMethods();

                // 遍历方法，检查是否有非标准的 setter 方法
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.startsWith(SET_PREFIX) && method.getParameterCount() == 1) {
                        Class<?> methodReturnType = method.getReturnType();
                        if(methodReturnType == void.class || methodReturnType == currClazz) {
                            // 如果方法以 "set" 开头，只有一个参数，并且返回类型是 void或者本类对象，则认为是 setter 方法
                            String propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                            PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, null, method);
                            setters.add(descriptor);
                        }
                    }
                }

                // 获取父类并继续迭代
                currClazz = currClazz.getSuperclass();
            }
        } catch (Exception e) {
            // 处理异常，根据需要进行日志记录或其他操作
            throw new CodeGenerationException(e);
        }

        if(!setters.isEmpty()) {
            try {
                PropertyDescriptor[] beanGetters = ReflectUtils.getBeanGetters(clazz);
                for (PropertyDescriptor setter : setters) {
                    PropertyDescriptor propertyDescriptor = Arrays.stream(beanGetters).filter(e ->
                            Objects.equals(e.getName(), setter.getName())
                    ).findAny().orElse(null);
                    if(propertyDescriptor != null) {
                        setter.setReadMethod(propertyDescriptor.getReadMethod());
                    }

                }
            } catch (IntrospectionException e) {
                // 处理异常，根据需要进行日志记录或其他操作
                throw new CodeGenerationException(e);
            }

        }

        return setters.toArray(new PropertyDescriptor[0]);
    }
}

