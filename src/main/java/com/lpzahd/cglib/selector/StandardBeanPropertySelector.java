package com.lpzahd.cglib.selector;


import com.lpzahd.cglib.BeanPropertySelector;
import net.sf.cglib.core.ReflectUtils;

import java.beans.PropertyDescriptor;

/**
 * Class Name: StandardBeanPropertySelector
 * Package: com.lpzahd.ease.framework.cglib.selector
 * Description: 遵循 JavaBean 命名约定的标准 JavaBean 类的属性选择器
 * @author lpzahd
 * Create DateTime: 2023/9/9 12:01
 * Version: 1.0
 */
public class StandardBeanPropertySelector implements BeanPropertySelector {

    @Override
    public PropertyDescriptor[] selectGetters(Class<?> clazz) {
        // 基于标准 JavaBean 命名约定选择属性获取器
        PropertyDescriptor[] beanGetters = ReflectUtils.getBeanGetters(clazz);
        PropertyDescriptor[] getters = new PropertyDescriptor[beanGetters.length];
        System.arraycopy(beanGetters, 0, getters, 0, beanGetters.length);
        return getters;
    }

    @Override
    public PropertyDescriptor[] selectSetters(Class<?> clazz) {
        // 基于标准 JavaBean 命名约定选择属性设置器
        PropertyDescriptor[] beanSetters = ReflectUtils.getBeanSetters(clazz);
        PropertyDescriptor[] setters = new PropertyDescriptor[beanSetters.length];
        System.arraycopy(beanSetters, 0, setters, 0, beanSetters.length);
        return setters;
    }
}

