package com.lpzahd.cglib;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import net.sf.cglib.core.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.security.ProtectionDomain;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Class Name: Copier
 * Package: com.lpzahd.ease.framework.cglib
 * Description: 拷贝实现，指令参考https://blog.csdn.net/yueyemaitian/article/details/84406057
 *
 * @author lpzahd
 * Create DateTime: 2023/8/31 9:26
 * Version: 1.0.0
 */
public abstract class Copier {

    private static final BeanCopierKey KEY_FACTORY = (BeanCopierKey) KeyFactory.create(BeanCopierKey.class);

    private static final Type CONVERTER = TypeUtils.parseType(Converter.class.getName());
    private static final Type FILTER = TypeUtils.parseType(Filter.class.getName());
    private static final Type BEAN_COPIER = TypeUtils.parseType(Copier.class.getName());
    private static final Signature COPY = new Signature("copy", Type.VOID_TYPE, new Type[]{Constants.TYPE_OBJECT, Constants.TYPE_OBJECT, FILTER, CONVERTER});
    private static final Signature CONVERT = TypeUtils.parseSignature("Object convert(Object, Class, Object)");
    private static final Signature FILTER_SIGN = TypeUtils.parseSignature("boolean accept(Object, String, Object, String)");

    public static Copier create(Class<?> source, Class<?> target, boolean useConverter) {
        return create(source, target, false, useConverter, Collections.emptyMap(), null);
    }

    public static Copier create(Class<?> source, Class<?> target, Map<String, String> mapper) {
        return create(source, target, false, false, mapper, null);
    }

    public static Copier create(Class<?> source, Class<?> target, boolean useFilter, boolean useConverter, Map<String, String> mapper, BeanPropertySelector selector) {
        Generator gen = new Generator();
        gen.setSource(source);
        gen.setTarget(target);
        gen.setUseFilter(useFilter);
        gen.setUseConverter(useConverter);
        gen.setSelector(selector);
        if (!mapper.isEmpty()) {
            gen.setMapper(Collections.unmodifiableMap(mapper));
        }
        return gen.create();
    }

    public abstract void copy(Object source, Object target, Filter<?, ?> filter, Converter converter);

    /**
     * 内部字节码代码生成
     */
    public static class Generator extends AbstractClassGenerator<Copier> {

        private static final Source SOURCE = new Source(Copier.class.getName());

        private Class<?> source;
        private Class<?> target;
        private boolean useFilter = false;
        private boolean useConverter = false;
        private Map<String, String> mapper = Collections.emptyMap();

        private BeanPropertySelector selector = BeanPropertySelector.STANDARD_SELECTOR;

        Generator() {
            super(SOURCE);
        }

        public void setSelector(BeanPropertySelector selector) {
            if (selector != null) {
                this.selector = selector;
            }
        }

        public void setSource(Class<?> source) {
            if (!Modifier.isPublic(source.getModifiers())) {
                setNamePrefix(source.getName());
            }
            this.source = source;
        }

        public void setTarget(Class<?> target) {
            if (!Modifier.isPublic(target.getModifiers())) {
                setNamePrefix(target.getName());
            }
            this.target = target;
        }

        public void setUseFilter(boolean useFilter) {
            this.useFilter = useFilter;
        }

        public void setUseConverter(boolean useConverter) {
            this.useConverter = useConverter;
        }

        public void setMapper(Map<String, String> mapper) {
            this.mapper = mapper;
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return source.getClassLoader();
        }

        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(source);
        }

        public Copier create() {
            BeanCopierKey key = (BeanCopierKey) KEY_FACTORY.newInstance(source.getName(), target.getName(), useFilter, useConverter);
            return (Copier) super.create(key);
        }

        /**
         * MethodName: getBeanGetters
         * Description: 获取bean的getter方法
         *
         * @author lpzahd
         * Create DateTime: 2023/9/9 13:18
         * Version: 1.0
         */
        private PropertyDescriptor[] getBeanGetters(Class<?> clz) {
            return selector.selectGetters(clz);
        }

        /**
         * MethodName: getBeanSetters
         * Description: 获取bean的setter方法
         *
         * @author lpzahd
         * Create DateTime: 2023/9/9 12:08
         * Version: 1.0
         */
        private PropertyDescriptor[] getBeanSetters(Class<?> clz) {
            return selector.selectSetters(clz);
        }

        @Override
        public void generateClass(ClassVisitor v) {
            Type sourceType = Type.getType(source);
            Type targetType = Type.getType(target);
            ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(46, 1, getClassName(), BEAN_COPIER, null, "<generated>");
            EmitUtils.null_constructor(ce);
            CodeEmitter e = ce.begin_method(1, COPY, null);
            PropertyDescriptor[] getters = getBeanGetters(source);
            PropertyDescriptor[] setters = getBeanSetters(target);
            Map<String, PropertyDescriptor> sourceGetPropertyNames = new HashMap<>(getters.length);
            for (PropertyDescriptor getter : getters) {
                sourceGetPropertyNames.put(getter.getName(), getter);
            }

            // 加载源数据
            Local sourceLocal = e.make_local();
            e.load_arg(0);
            e.checkcast(sourceType);
            e.store_local(sourceLocal);

            // 加载目标数据
            Local targetLocal = e.make_local();
            e.load_arg(1);
            e.checkcast(targetType);
            e.store_local(targetLocal);

            visitInnerProperties(e, sourceGetPropertyNames, setters, sourceLocal, targetLocal);

            e.return_value();
            e.end_method();
            ce.end_class();
        }

        /**
         * MethodName: visitInnerProperties
         * Description: 迭代内部属性
         *
         * @param e                      代码生成器
         * @param sourceGetPropertyNames 源对象所有getter属性以及映射的名称
         * @param targetSetProperties    目标对象属性的所有setter属性
         * @param sourceLocal            源对象本地变量
         * @param targetLocal            目标对象本地变量
         * @author lpzahd
         * Create DateTime: 2023/9/6 20:50
         * Version: 1.0
         */
        private void visitInnerProperties(CodeEmitter e, Map<String, PropertyDescriptor> sourceGetPropertyNames, PropertyDescriptor[] targetSetProperties, Local sourceLocal, Local targetLocal) {
            for (PropertyDescriptor targetSetProperty : targetSetProperties) {
                PropertyDescriptor sourceGetProperty = (mapper.isEmpty()) ? sourceGetPropertyNames.get(targetSetProperty.getName()) : sourceGetPropertyNames.get(mapper.get(targetSetProperty.getName()));
                if (sourceGetProperty != null) {
                    MethodInfo sourceRead = ReflectUtils.getMethodInfo(sourceGetProperty.getReadMethod());
                    MethodInfo targetWrite = ReflectUtils.getMethodInfo(targetSetProperty.getWriteMethod());
                    Class<?> sourcePropertyClass = sourceGetProperty.getPropertyType();
                    Class<?> targetPropertyClass = targetSetProperty.getPropertyType();
                    if (Collection.class.isAssignableFrom(targetPropertyClass) && Collection.class.isAssignableFrom(sourcePropertyClass)) {
                        // 当前是集合
                        // 获取集合属性的泛型类型
                        Class<?> sourceGenericClass = getCollectionGenericType(sourceGetProperty.getWriteMethod().getGenericParameterTypes()[0]);
                        Class<?> targetGenericClass = getCollectionGenericType(targetSetProperty.getWriteMethod().getGenericParameterTypes()[0]);

                        if (sourceGenericClass != null && targetGenericClass != null) {
                            // 目标要生成的类型
                            Class<?> targetCollectionClass;
                            if (isConcreteClass(targetPropertyClass)) {
                                // 该类是具体实现类
                                targetCollectionClass = targetPropertyClass;
                            } else {
                                if (List.class.isAssignableFrom(targetPropertyClass)) {
                                    targetCollectionClass = ArrayList.class;
                                } else if (Set.class.isAssignableFrom(targetPropertyClass)) {
                                    targetCollectionClass = HashSet.class;
                                } else {
                                    // 类型不明确，暂时用ArrayList.class;
                                    targetCollectionClass = ArrayList.class;
                                }
                            }

                            // 生成代码实现集合属性的深拷贝
                            deepCopyCollection(
                                    e,
                                    sourceLocal, targetLocal,
                                    sourceRead, targetWrite,
                                    sourceGenericClass, targetGenericClass,
                                    Collection.class, targetCollectionClass
                            );
                        } else {
                            // 泛型没有匹配到，则直接进行赋值
                            copyValue(e, sourceLocal, targetLocal, targetSetProperty, sourceGetProperty, sourceRead, targetWrite);
                        }
                    } else if (Map.class.isAssignableFrom(targetPropertyClass) && Map.class.isAssignableFrom(sourcePropertyClass)) {
                        // 当前是集合
                        // 获取集合属性的泛型类型
                        MapTypeInformation sourceTypeInformation = getMapGenericType(sourceGetProperty.getWriteMethod().getGenericParameterTypes()[0]);
                        MapTypeInformation targetTypeInformation = getMapGenericType(targetSetProperty.getWriteMethod().getGenericParameterTypes()[0]);

                        if (sourceTypeInformation != null && targetTypeInformation != null) {
                            // 目标要生成的类型
                            Class<?> targetMapClass;
                            if (isConcreteClass(targetPropertyClass)) {
                                // 该类是具体实现类
                                targetMapClass = targetPropertyClass;
                            } else {
                                if (Map.class.isAssignableFrom(targetPropertyClass)) {
//                                    targetMapClass = TreeMap.class;
                                    targetMapClass = HashMap.class;
                                } else {
                                    // 类型不明确，暂时用HashSet.class;
                                    targetMapClass = HashSet.class;
                                }
                            }

                            deepCopyMap(
                                    e,
                                    sourceLocal, targetLocal,
                                    sourceRead, targetWrite,
                                    sourceTypeInformation.getKeyType(), sourceTypeInformation.getValueType(),
                                    targetTypeInformation.getKeyType(), targetTypeInformation.getValueType(),
                                    Map.class, targetMapClass
                            );
                        } else {
                            // 泛型没有匹配到，则直接进行赋值
                            copyValue(e, sourceLocal, targetLocal, targetSetProperty, sourceGetProperty, sourceRead, targetWrite);
                        }
                    } else if (targetPropertyClass.isArray() && sourcePropertyClass.isArray()) {
                        // 当前属性是数组
                        deepCopyArray(e, sourceLocal, targetLocal, sourceRead, targetWrite, sourcePropertyClass, targetPropertyClass);
                    } else {
                        boolean isBean = guessBean(targetPropertyClass);
                        if (isBean) {
                            // 获取当前对象的引用
                            Local sourceValueLocal = e.make_local(sourceRead.getSignature().getReturnType());
                            e.load_local(sourceLocal);
                            e.invoke(sourceRead);
                            e.store_local(sourceValueLocal);


                            // 检查源值是否为 null
                            Label notNullLabel = e.make_label();
                            e.load_local(sourceValueLocal);
                            e.ifnonnull(notNullLabel);

                            // 如果源值为 null，则跳过赋值操作
                            // 否则继续执行
                            Label endLabel = e.make_label();
                            e.goTo(endLabel);

                            // 标记非 null 时的代码块开始
                            e.mark(notNullLabel);

                            // 创建一个新的对象
                            Type targetValueType = Type.getType(targetPropertyClass);
                            Local targetValueLocal = e.make_local(targetValueType);
                            e.new_instance(targetValueType);
                            e.dup();
                            e.invoke_constructor(targetValueType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
                            e.store_local(targetValueLocal);

                            // 目标对象set新创建的对象
                            e.load_local(targetLocal);
                            e.load_local(targetValueLocal);
                            e.invoke(targetWrite);

                            // 赋值
                            innerCopy(e, sourcePropertyClass, targetPropertyClass, sourceValueLocal, targetValueLocal);

                            // 标记代码块结束
                            e.mark(endLabel);
                        } else {
                            copyValue(e, sourceLocal, targetLocal, targetSetProperty, sourceGetProperty, sourceRead, targetWrite);
                        }
                    }
                }
            }
        }

        private void copyValue(CodeEmitter e, Local sourceLocal, Local targetLocal, PropertyDescriptor targetSetProperty, PropertyDescriptor sourceGetProperty, MethodInfo sourceRead, MethodInfo targetWrite) {
            if (useFilter) {
                if (useConverter) {
                    filterInvoke(e, sourceLocal, targetLocal, sourceGetProperty, targetSetProperty, sourceRead, targetWrite);
                } else if (compatible(sourceGetProperty, targetSetProperty)) {
                    MethodInfo targetRead = ReflectUtils.getMethodInfo(targetSetProperty.getReadMethod());
                    e.load_arg(2);
                    e.load_local(sourceLocal);
                    e.invoke(sourceRead);
                    e.box(sourceRead.getSignature().getReturnType());
                    e.push(targetSetProperty.getName());
                    e.load_local(targetLocal);
                    e.invoke(targetRead);
                    e.box(targetRead.getSignature().getReturnType());
                    e.push(sourceGetProperty.getName());
                    e.invoke_interface(FILTER, FILTER_SIGN);
                    Label ifLabel = e.make_label();
                    e.if_jump(Opcodes.IFEQ, ifLabel);
                    e.load_local(targetLocal);
                    e.load_local(sourceLocal);
                    e.invoke(sourceRead);
                    e.invoke(targetWrite);
                    if (targetWrite.getSignature().getReturnType() != Type.VOID_TYPE) {
                        e.pop();
                    }
                    e.visitLabel(ifLabel);
                }
            } else {
                if (useConverter) {
                    convertInvoke(e, sourceLocal, targetLocal, sourceRead, targetWrite);
                } else if (compatible(sourceGetProperty, targetSetProperty)) {
                    writeInvoke(e, sourceLocal, targetLocal, sourceRead, targetWrite);
                }
            }
        }


        private void filterInvoke(CodeEmitter e, Local sourceLocal, Local targetLocal, PropertyDescriptor getter, PropertyDescriptor setter, MethodInfo read, MethodInfo write) {
            MethodInfo targetRead = ReflectUtils.getMethodInfo(setter.getReadMethod());
            e.load_arg(2);
            e.load_local(sourceLocal);
            e.invoke(read);
            e.box(read.getSignature().getReturnType());
            e.push(setter.getName());
            e.load_local(targetLocal);
            e.invoke(targetRead);
            e.box(targetRead.getSignature().getReturnType());
            e.push(getter.getName());
            e.invoke_interface(FILTER, FILTER_SIGN);
            Label ifLabel = e.make_label();
            e.if_jump(Opcodes.IFEQ, ifLabel);
            convertInvoke(e, sourceLocal, targetLocal, read, write);
            e.visitLabel(ifLabel);
        }

        private void writeInvoke(CodeEmitter e, Local sourceLocal, Local targetLocal, MethodInfo read, MethodInfo write) {
//            e.load_local(sourceLocal);
//            e.invoke(read); // 将属性值加载到栈上
//            e.load_local(targetLocal);
//            e.swap();
//            e.invoke(write); // 写入目标对象

            Local tempLocal = e.make_local(read.getSignature().getReturnType());
            e.load_local(sourceLocal);  // 将 sourceLocal 对应的值加载到栈上
            e.invoke(read);              // 调用 read 方法，将属性值加载到栈上
            e.store_local(tempLocal);    // 将属性值存储到临时变量中
            e.load_local(targetLocal);  // 将 targetLocal 对应的值加载到栈上
            e.load_local(tempLocal);     // 将临时变量中的值加载到栈上
            e.invoke(write);             // 调用 write 方法，将属性值写入目标对象

            if (write.getSignature().getReturnType() != Type.VOID_TYPE) {
                e.pop();
            }
        }

        private void convertInvoke(CodeEmitter e, Local sourceLocal, Local targetLocal, MethodInfo read, MethodInfo write) {
            Type setterType = write.getSignature().getArgumentTypes()[0];
            e.load_local(targetLocal);
            e.load_arg(3);
            e.load_local(sourceLocal);
            e.invoke(read);
            e.box(read.getSignature().getReturnType());
            EmitUtils.load_class(e, setterType);
            e.push(write.getSignature().getName());
            e.invoke_interface(CONVERTER, CONVERT);
            e.unbox_or_zero(setterType);
            e.invoke(write);
            if (write.getSignature().getReturnType() != Type.VOID_TYPE) {
                e.pop();
            }
        }

        // 获取集合属性的泛型类型
        private Class<?> getCollectionGenericType(java.lang.reflect.Type type) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    java.lang.reflect.Type genericType = actualTypeArguments[0];
                    if (genericType instanceof Class) {
                        return (Class<?>) genericType;
                    }
                }
            }
            return null;
        }

        // 获取Map属性的泛型类型
        private MapTypeInformation getMapGenericType(java.lang.reflect.Type type) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 1) {
                    java.lang.reflect.Type genericKeyType = actualTypeArguments[0];
                    java.lang.reflect.Type genericValueType = actualTypeArguments[1];
                    if (genericKeyType instanceof Class && genericValueType instanceof Class) {
                        return new MapTypeInformation((Class<?>) genericKeyType, (Class<?>) genericValueType);
                    }
                }
            }
            return null;
        }

        // 生成集合属性的深拷贝代码
        private void deepCopyCollection(
                CodeEmitter e,
                Local sourceLocal, Local targetLocal,
                MethodInfo read, MethodInfo write,
                Class<?> sourceGenericClass, Class<?> targetGenericClass,
                Class<?> collectionClass, Class<?> collectionImplClass
        ) {
            // collection接口的Type类型
            Type collectionType = Type.getType(collectionClass);
            // collection类的Type类型
            Type collectionImplType = Type.getType(collectionImplClass);
            // iterator接口的Type类型
            Type iteratorType = Type.getType(Iterator.class);
            // 源对象的Type类型
            Type sourceGenericType = Type.getType(sourceGenericClass);
            // 目标对象的Type类型
            Type targetGenericType = Type.getType(targetGenericClass);

            Local sourceCollectionLocal = e.make_local(collectionType); // 存储源集合属性
            Local elementLocal = e.make_local(sourceGenericType); // 存储集合元素

            Label loopStart = e.make_label();
            Label loopEnd = e.make_label();

            // 生成代码：List<T> sourceCollectionLocal = source.getCollectionProperty();
            e.load_local(sourceLocal);
            e.invoke(read);
            e.checkcast(collectionType);
            e.store_local(sourceCollectionLocal);

            // 在非null的情况下处理
            e.load_local(sourceCollectionLocal);
            Label notNullLabel = new Label();
            e.ifnull(notNullLabel);

            // 创建一个新的Collection
            Local newCollectionLocal = e.make_local(collectionImplType);
            e.new_instance(collectionImplType);
            e.dup();
            e.invoke_constructor(collectionImplType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
            e.store_local(newCollectionLocal);

            // 获取迭代器 iterator = sourceCollection.iterator()
            e.load_local(sourceCollectionLocal);
            e.invoke_interface(collectionType, new Signature("iterator", iteratorType, new Type[0]));
            Local iteratorLocal = e.make_local(iteratorType);
            e.store_local(iteratorLocal);

            // Start of the loop
            e.mark(loopStart);

            // iterator.hasNex()
            e.load_local(iteratorLocal);
            e.invoke_interface(iteratorType, new Signature("hasNext", Type.BOOLEAN_TYPE, new Type[0]));
            e.if_jump(Opcodes.IFEQ, loopEnd); // If hasNext() returns false, exit the loop

            // iterator.next()
            e.load_local(iteratorLocal);
            e.invoke_interface(iteratorType, new Signature("next", Type.getType(Object.class), new Type[0]));
            e.checkcast(sourceGenericType);
            e.store_local(elementLocal);

            // 递归拷贝
            Local targetElementLocal;
            if (!guessBean(targetGenericClass)) {
                targetElementLocal = createAndStoreLocalVariable(e, elementLocal, targetGenericClass);
            } else {
                targetElementLocal = e.make_local(targetGenericType);
                e.new_instance(targetGenericType);
                e.dup();
                e.invoke_constructor(targetGenericType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
                e.store_local(targetElementLocal);
                innerCopy(e, sourceGenericClass, targetGenericClass, elementLocal, targetElementLocal);
            }

            // newCollection.add()
            e.load_local(newCollectionLocal);
            e.load_local(targetElementLocal);
            e.invoke_interface(collectionType, TypeUtils.parseSignature("boolean add(Object)"));
            e.pop(); // Pop the result (boolean)

            // Repeat the loop
            e.goTo(loopStart);

            // End of the loop
            e.mark(loopEnd);

            // 赋值
//            e.load_local(newCollectionLocal);
//            e.load_local(targetLocal);
//            e.swap();
//            e.invoke(write);

            e.load_local(targetLocal);
            e.load_local(newCollectionLocal);
            e.invoke(write);

            if (write.getSignature().getReturnType() != Type.VOID_TYPE) {
                e.pop();
            }

            e.mark(notNullLabel);
        }

        // 处理数组属性的深拷贝逻辑
        private void deepCopyArray(CodeEmitter e, Local sourceLocal, Local targetLocal, MethodInfo read, MethodInfo write, Class<?> sourceClass, Class<?> targetClass) {
            // 源对象Type类型
            Type sourceType = Type.getType(sourceClass);
            // 目标对象Type类型
            Type targetType = Type.getType(targetClass);
            // 源对象数组Type类型
            Type sourceComponentType = Type.getType(sourceClass.getComponentType());
            // 目标对象数组Type类型
            Type targetComponentType = Type.getType(targetClass.getComponentType());

            Label loopStart = e.make_label();
            Label loopEnd = e.make_label();

            // 生成代码：String[] sourceArray = source.getArray();
            Local sourceArrayLocal = e.make_local(sourceType);
            e.load_local(sourceLocal);
            e.invoke(read);
            e.checkcast(Type.getType(sourceClass));
            e.store_local(sourceArrayLocal);

            // 在非null的情况下处理
            e.load_local(sourceArrayLocal);
            Label notNullLabel = new Label();
            e.ifnull(notNullLabel);

            // 生成代码：int sourceArrayLength = sourceArray.length;
            e.load_local(sourceArrayLocal);
            e.arraylength();
            Local sourceArrayLength = e.make_local(Type.INT_TYPE);
            e.store_local(sourceArrayLength);

            // 生成代码：newArray = new TargetType[sourceArrayLength];
            Local newArrayLocal = e.make_local(targetType);
            e.load_local(sourceArrayLength);
            e.newarray(targetComponentType);
            e.store_local(newArrayLocal);

            // 初始化循环变量
            e.push(0);
            Local indexLocal = e.make_local(Type.INT_TYPE);
            e.store_local(indexLocal);

            // 开始循环
            e.mark(loopStart);

            // 生成代码：if (i < sourceArrayLength) { ... }
            e.load_local(indexLocal);
            e.load_local(sourceArrayLength);
            e.if_jump(Opcodes.IF_ICMPGE, loopEnd);

            // 生成代码：element = sourceArray[i];
            e.load_local(sourceArrayLocal);
            e.load_local(indexLocal);
            Local elementLocal = e.make_local(sourceComponentType);
            e.array_load(sourceComponentType);
            e.store_local(elementLocal);

            // 递归拷贝
            Local targetElementLocal;
            if (!guessBean(targetClass.getComponentType())) {
                targetElementLocal = createAndStoreLocalVariable(e, elementLocal, targetClass.getComponentType());
            } else {
                targetElementLocal = e.make_local(targetComponentType);
                e.new_instance(targetComponentType);
                e.dup();
                e.invoke_constructor(targetComponentType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
                e.store_local(targetElementLocal);
                innerCopy(e, sourceClass.getComponentType(), targetClass.getComponentType(), elementLocal, targetElementLocal);
            }

//            // 将拷贝后的元素存储到目标数组中
//            e.load_local(newArrayLocal);
//            e.load_local(targetElementLocal);
//            e.load_local(indexLocal);
//            e.swap(); // 将源元素复制到栈顶
//            e.array_store(targetComponentType);

            // 将拷贝后的元素存储到目标数组中
            e.load_local(newArrayLocal);
            e.load_local(indexLocal);
            e.load_local(targetElementLocal);
            e.array_store(targetComponentType);

            // 生成代码：i++
            e.iinc(indexLocal, 1);
            e.goTo(loopStart);

            // 循环结束
            e.mark(loopEnd);

            // 赋值
//            e.load_local(newArrayLocal);
//            e.load_local(targetLocal);
//            e.swap();
//            e.invoke(write);

            e.load_local(targetLocal);
            e.load_local(newArrayLocal);
            e.invoke(write);

            if (write.getSignature().getReturnType() != Type.VOID_TYPE) {
                e.pop();
            }

            e.mark(notNullLabel);
        }


        private void deepCopyMap(
                CodeEmitter e,
                Local sourceLocal, Local targetLocal,
                MethodInfo read, MethodInfo write,
                Class<?> sourceKeyClass, Class<?> sourceValueClass,
                Class<?> targetKeyClass, Class<?> targetValueClass,
                Class<?> mapClass, Class<?> mapImplClass
        ) {
            // map接口的Type类型
            Type mapType = Type.getType(mapClass);
            // map类的Type类型
            Type mapImplType = Type.getType(mapImplClass);
            // iterator的Type类型
            Type iteratorType = Type.getType(Iterator.class);
            // Map.Entry的Type类型
            Type mapEntryType = Type.getType(Map.Entry.class);
            // set的Type类型
            Type setType = Type.getType(Set.class);
            // object的Type类型
            Type objectType = Type.getType(Object.class);
            // 源对象key的Type类型
            Type sourceKeyType = Type.getType(sourceKeyClass);
            // 源对象value的Type类型
            Type sourceValueType = Type.getType(sourceValueClass);
            // 目标对象key的Type类型
            Type targetKeyType = Type.getType(targetKeyClass);
            // 目标对象value的Type类型
            Type targetValueType = Type.getType(targetValueClass);

            Local sourceMapLocal = e.make_local(mapType); // 存储源 Map 属性
            Local entrySetLocal = e.make_local(setType); // 存储 Map.Entry 集合
            Local entryIteratorLocal = e.make_local(iteratorType); // 存储迭代器
            Local entryLocal = e.make_local(mapEntryType); // 存储 Map.Entry

            // Labels
            Label loopStart = e.make_label();
            Label loopEnd = e.make_label();

            // 生成代码：Map<K, V> sourceMapLocal = source.getMapProperty();
            e.load_local(sourceLocal);
            e.invoke(read);
            e.checkcast(mapType);
            e.store_local(sourceMapLocal);

            // 在非null的情况下处理
            e.load_local(sourceMapLocal);
            Label notNullLabel = new Label();
            e.ifnull(notNullLabel);

            // 创建一个新的Map
            Local newMapLocal = e.make_local(mapImplType);
            e.new_instance(mapImplType);
            e.dup();
            e.invoke_constructor(mapImplType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
            e.store_local(newMapLocal);

            // 获取 Map.Entry 集合
            e.load_local(sourceMapLocal);
            e.invoke_interface(mapType, new Signature("entrySet", setType, new Type[0]));
            e.store_local(entrySetLocal);

            // 获取迭代器
            e.load_local(entrySetLocal);
            e.invoke_interface(setType, new Signature("iterator", iteratorType, new Type[0]));
            e.store_local(entryIteratorLocal);

            // 开始迭代
            e.mark(loopStart);

            // 检查是否有下一个元素
            e.load_local(entryIteratorLocal);
            e.invoke_interface(iteratorType, new Signature("hasNext", Type.BOOLEAN_TYPE, new Type[0]));
            e.if_jump(Opcodes.IFEQ, loopEnd);


            // 获取下一个 Map.Entry
            e.load_local(entryIteratorLocal);
            e.invoke_interface(iteratorType, new Signature("next", objectType, new Type[0]));
            e.checkcast(mapEntryType);
            e.store_local(entryLocal);

            // 获取键和值
            e.load_local(entryLocal);
            e.invoke_interface(mapEntryType, new Signature("getKey", objectType, new Type[0]));
            e.checkcast(sourceKeyType);
            Local targetKeyTypeLocal = e.make_local(targetKeyType); // 存储键的深拷贝
            e.store_local(targetKeyTypeLocal);

            e.load_local(entryLocal);
            e.invoke_interface(mapEntryType, new Signature("getValue", objectType, new Type[0]));
            e.checkcast(sourceValueType);
            Local targetValueTypeLocal = e.make_local(targetValueType); // 存储值的深拷贝
            e.store_local(targetValueTypeLocal);

            // 递归拷贝
            Local targetElementKeyLocal;
            if (!guessBean(targetKeyClass)) {
                targetElementKeyLocal = createAndStoreLocalVariable(e, targetKeyTypeLocal, targetKeyClass);
            } else {
                targetElementKeyLocal = e.make_local(targetKeyType);
                e.new_instance(targetKeyType);
                e.dup();
                e.invoke_constructor(targetKeyType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
                e.store_local(targetElementKeyLocal);
                innerCopy(e, sourceKeyClass, targetKeyClass, targetKeyTypeLocal, targetElementKeyLocal);
            }

            // 递归拷贝
            Local targetElementValueLocal;
            if (!guessBean(targetValueClass)) {
                targetElementValueLocal = createAndStoreLocalVariable(e, targetValueTypeLocal, targetValueClass);
            } else {
                targetElementValueLocal = e.make_local(targetValueType);
                e.new_instance(targetValueType);
                e.dup();
                e.invoke_constructor(targetValueType, new Signature("<init>", Type.VOID_TYPE, new Type[0]));
                e.store_local(targetElementValueLocal);
                innerCopy(e, sourceValueClass, targetValueClass, targetValueTypeLocal, targetElementValueLocal);
            }

            // 在目标 Map 中添加键值对
            e.load_local(newMapLocal);
            e.load_local(targetElementKeyLocal);
            e.load_local(targetElementValueLocal);
            e.invoke_interface(mapType, new Signature("put", objectType, new Type[]{objectType, objectType}));
            e.pop();

            // 回到迭代的开始
            e.goTo(loopStart);

            // 结束迭代
            e.mark(loopEnd);

            // 赋值
//            e.load_local(newMapLocal);
//            e.load_local(targetLocal);
//            e.swap();
//            e.invoke(write);

            e.load_local(targetLocal);
            e.load_local(newMapLocal);
            e.invoke(write);
            if (write.getSignature().getReturnType() != Type.VOID_TYPE) {
                e.pop();
            }

            e.mark(notNullLabel);
        }


        private void innerCopy(CodeEmitter e, Class<?> source, Class<?> target, Local sourceLocal, Local targetLocal) {
            PropertyDescriptor[] getters = getBeanGetters(source);
            PropertyDescriptor[] setters = getBeanSetters(target);
            Map<String, PropertyDescriptor> sourceGetPropertyNames = new HashMap<>(getters.length);
            for (PropertyDescriptor getter : getters) {
                sourceGetPropertyNames.put(getter.getName(), getter);
            }

            visitInnerProperties(e, sourceGetPropertyNames, setters, sourceLocal, targetLocal);
        }

        /**
         * MethodName: storeTargetElementLocal
         * Description: 将源元素的值存储到一个新的本地变量中，并返回该新的本地变量。
         *
         * @param e                  代码生成器
         * @param sourceElementLocal 源元素的本地变量
         * @param targetClass        目标元素的类型（类对象）
         * @author lpzahd
         * Create DateTime: 2023/9/6 22:34
         * Version: 1.0
         */
        private Local createAndStoreLocalVariable(CodeEmitter e, Local sourceElementLocal, Class<?> targetClass) {
            Type targetType = Type.getType(targetClass);
            Local targetElementLocal = e.make_local(targetType);
            if (targetClass.isPrimitive() || Number.class.isAssignableFrom(targetClass)
                    || Character.class == targetClass || String.class == targetClass || Boolean.class == targetClass
                    || Temporal.class.isAssignableFrom(targetClass)) {
                // 一些简单类型，直接赋值
                e.load_local(sourceElementLocal);
                e.store_local(targetElementLocal);
            } else if (Date.class.isAssignableFrom(targetClass)) {
                // long time = date.getTime()
                e.load_local(sourceElementLocal);
                e.invoke_interface(Type.getType(Date.class), new Signature("getTime", Type.getType(Long.TYPE), new Type[0]));
                Local timeLocal = e.make_local(Type.getType(Long.TYPE));
                e.store_local(timeLocal);

                // Date element = new Date(time)
                e.new_instance(targetType);
                e.dup();
                e.load_local(timeLocal);  // 将时间值加载到栈上
                e.invoke_constructor(targetType, new Signature("<init>", Type.VOID_TYPE, new Type[]{Type.getType(Long.TYPE)}));
                e.store_local(targetElementLocal);
            } else {
                // 没有缺失的类型判断，直接赋值
                e.load_local(sourceElementLocal);
                e.store_local(targetElementLocal);
            }
            return targetElementLocal;
        }

        @Override
        protected Object firstInstance(Class type) {
            return ReflectUtils.newInstance(type);
        }

        @Override
        protected Object nextInstance(Object instance) {
            return instance;
        }

        private boolean compatible(PropertyDescriptor getter, PropertyDescriptor setter) {
            return setter.getPropertyType().isAssignableFrom(getter.getPropertyType());
        }

        /**
         * 猜测是不是bean
         */
        private boolean guessBean(Class<?> clz) {
            if (clz.isPrimitive()) {
                return false;
            }
            if (clz.isArray()) {
                return false;
            }
            if (Collection.class.isAssignableFrom(clz)) {
                return false;
            }
            if (Map.class.isAssignableFrom(clz)) {
                return false;
            }
            if (Number.class.isAssignableFrom(clz)) {
                return false;
            }
            if (Boolean.class.isAssignableFrom(clz)) {
                return false;
            }
            if (Character.class == clz) {
                return false;
            }
            if (String.class == clz) {
                return false;
            }
            if (Date.class.isAssignableFrom(clz)) {
                return false;
            }
            if (Temporal.class.isAssignableFrom(clz)) {
                return false;
            }
            return Object.class != clz;
        }

        /**
         * 判断类是不是一个的实现类
         */
        public static boolean isConcreteClass(Class<?> clazz) {
            boolean isConcreteClass = !clazz.isInterface() && !clazz.isEnum() && !clazz.isAnnotation() && !clazz.isArray() &&
                    !clazz.isPrimitive() && !clazz.isSynthetic() && !clazz.isLocalClass() && !clazz.isAnonymousClass();
            if (!isConcreteClass) {
                return false;
            }
            return !Modifier.isAbstract(clazz.getModifiers());
        }


    }

    public interface BeanCopierKey {

        /**
         * Method Name: newInstance
         * Description: 创建对象
         *
         * @param sourceClassName 源类名
         * @param targetClassName 目标类名
         * @param useFilter       使用Filter
         * @param useConvert      使用Convert
         * @author lpzahd
         * Create DateTime: 2023/8/30 9:55
         * Version: 1.0.0
         */
        Object newInstance(String sourceClassName, String targetClassName, boolean useFilter, boolean useConvert);

    }

    @Data
    @AllArgsConstructor
    public static class MapTypeInformation {

        // 保存Map键的Class类型
        private Class<?> keyType;

        // 保存Map值的Class类型
        private Class<?> valueType;

    }

}
