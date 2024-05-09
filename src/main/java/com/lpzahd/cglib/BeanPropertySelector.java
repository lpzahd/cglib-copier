package com.lpzahd.cglib;


import com.lpzahd.cglib.selector.NonStandardBeanPropertySelector;
import com.lpzahd.cglib.selector.StandardBeanPropertySelector;

import java.beans.PropertyDescriptor;

/**
 * Class Name: BeanPropertySelector
 * Package: com.lpzahd.ease.framework.cglib
 * Description: 接口定义了用于选择给定类属性的策略
 * @author lpzahd
 * Create DateTime: 2023/9/9 11:58
 * Version: 1.0
 */
public interface BeanPropertySelector {

    BeanPropertySelector STANDARD_SELECTOR = new StandardBeanPropertySelector();

    BeanPropertySelector NON_STANDARD_SELECTOR = new NonStandardBeanPropertySelector();


    /**
     * 选择指定类的属性获取器。
     *
     * @param clazz 应该选择属性获取器的 Class。
     * @return 表示所选属性获取器的 PropertyDescriptor 对象数组。
     */
    PropertyDescriptor[] selectGetters(Class<?> clazz);

    /**
     * 选择指定类的属性设置器。
     *
     * @param clazz 应该选择属性设置器的 Class。
     * @return 表示所选属性设置器的 PropertyDescriptor 对象数组。
     */
    PropertyDescriptor[] selectSetters(Class<?> clazz);

}
