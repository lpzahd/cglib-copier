package com.lpzahd.cglib;


import net.sf.cglib.core.Converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * Class Name: SmartConvert
 * Package: com.lpzahd.ease.framework.cglib
 * Description: 智能convert
 * @author lpzahd
 * Create DateTime: 2023/8/31 9:21
 * Version: 1.0.0
 */
public class SmartConvert implements Converter {

    private static final String DELIMITER = ",";

    private static final Class<?>[] BASIC_NUMBER_TYPE = {
            Byte.TYPE, Integer.TYPE, Short.TYPE, Long.TYPE, Double.TYPE, Float.TYPE
    };

    private static final Class<?>[] PACKAGE_NUMBER_TYPE = {
            Byte.class, Integer.class, Short.class, Long.class, Double.class, Float.class
    };

    private static boolean isNumberType(Class<?> clz) {
        return contains(BASIC_NUMBER_TYPE, clz) || contains(PACKAGE_NUMBER_TYPE, clz);
    }

    private static boolean contains(Class<?>[] cls, Class<?> clz) {
        for (Class<?> c : cls) {
            if (c == clz) {
                return true;
            }
        }
        return false;
    }

    private static boolean oneOfClass(Class<?> first, Class<?> second, Class<?> s) {
        return s == first || s == second;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convert(Object value, Class tClass, Object method) {
        if (value == null) {
            return null;
        }

        Class<?> sClass = value.getClass();

        if (tClass.isAssignableFrom(sClass)) {
            return value;
        }

        if (tClass.isPrimitive()) {
            if (tClass == Boolean.TYPE && sClass == Boolean.class) {
                return value;
            } else if (tClass == Character.TYPE && sClass == Character.class) {
                return value;
            } else if (tClass == Byte.TYPE && sClass == Byte.class) {
                return value;
            } else if (tClass == Short.TYPE && sClass == Short.class) {
                return value;
            } else if (tClass == Integer.TYPE && sClass == Integer.class) {
                return value;
            } else if (tClass == Float.TYPE && sClass == Float.class) {
                return value;
            } else if (tClass == Long.TYPE && sClass == Long.class) {
                return value;
            } else if (tClass == Double.TYPE && sClass == Double.class) {
                return value;
            }
        }

        if (sClass == String[].class && tClass == String.class) {
            return join(DELIMITER, Arrays.asList((String[]) value));
        }
        if (sClass == String.class && tClass == String[].class) {
            String stringValue = (String) value;
            return stringValue.isEmpty() ? new String[0] : stringValue.split(DELIMITER);
        }

        if (sClass == Long[].class && tClass == String.class) {
            return join(DELIMITER, Arrays.asList((Long[]) value));
        }
        if (sClass == String.class && tClass == Long[].class) {
            String stringValue = (String) value;
            if (stringValue.isEmpty()) {
                return new Long[0];
            } else {
                String[] split = stringValue.split(DELIMITER);
                Long[] result = new Long[split.length];
                for (int i = 0; i < split.length; i++) {
                    result[i] = Long.parseLong(split[i]);
                }
                return result;
            }
        }

        if (Iterable.class.isAssignableFrom(sClass) && tClass == String.class) {
            return join(DELIMITER, (Iterable<?>) value);
        }
        if (sClass == String.class && Iterable.class.isAssignableFrom(tClass)) {
            String stringValue = (String) value;
            if (stringValue.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Arrays.asList(stringValue.split(DELIMITER));
            }
        }

        if (Date.class.isAssignableFrom(sClass) && tClass == Long.class) {
            return ((Date) value).getTime();
        }
        if (sClass == Long.class && Date.class.isAssignableFrom(tClass)) {
            return new Date((Long) value);
        }

        if (LocalDateTime.class.isAssignableFrom(sClass) && tClass == Long.class) {
            LocalDateTime localDateTime = (LocalDateTime) value;
            return localDateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(localDateTime)).toEpochMilli();
        }
        if (sClass == Long.class && LocalDateTime.class.isAssignableFrom(tClass)) {
            Long longValue = (Long) value;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.systemDefault());
        }

        if (!String.class.isAssignableFrom(sClass) && tClass == String.class) {
            return value.toString();
        }

        if ((isNumberType(sClass) || sClass == String.class) && isNumberType(tClass)) {
            BigDecimal number = new BigDecimal(value.toString());
            if (oneOfClass(Byte.class, Byte.TYPE, tClass)) {
                return number.byteValue();
            }
            if (oneOfClass(Integer.class, Integer.TYPE, tClass)) {
                return number.intValue();
            }
            if (oneOfClass(Short.class, Short.TYPE, tClass)) {
                return number.shortValue();
            }
            if (oneOfClass(Long.class, Long.TYPE, tClass)) {
                return number.longValue();
            }
            if (oneOfClass(Double.class, Double.TYPE, tClass)) {
                return number.doubleValue();
            }
            if (oneOfClass(Float.class, Float.TYPE, tClass)) {
                return number.floatValue();
            }
            if (tClass == BigInteger.class) {
                return number.toBigInteger();
            }
            if (tClass == BigDecimal.class) {
                return number;
            }
            throw new IllegalArgumentException("请继续补充类型");
        }
        if (oneOfClass(Boolean.class, Boolean.TYPE, sClass) && oneOfClass(Boolean.class, Boolean.TYPE, tClass)) {
            return value;
        }
        if (isNumberType(sClass) && oneOfClass(Boolean.class, Boolean.TYPE, tClass)) {
            return new BigDecimal(value.toString()).compareTo(BigDecimal.ZERO) != 0;
        }
        return null;
    }

    private static String join(String delimiter, Iterable<?> values) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                result.append(delimiter);
            }
            result.append(value);
            first = false;
        }
        return result.toString();
    }
}
