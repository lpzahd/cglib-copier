package com.lpzahd.cglib;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.cglib.core.DebuggingClassWriter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class CopyTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Source {
        private Integer mInteger = 1;
        private int mInt = 1;
        private String mString = "1";
        private Character mCharacter = '1';
        private char mChar = '1';
        private Boolean mBoolean = true;
        private boolean mBooleanValue = true;
        private Long mLong = 1L;
        private long mLongValue = 1L;
        private Double mDouble = 1.0;
        private double mDoubleValue = 1.0;
        private Float mFloat = 1.0f;
        private float mFloatValue = 1.0f;
        private BigDecimal mBigDecimal = BigDecimal.ONE;
        private BigInteger mBigInteger = BigInteger.ONE;

        private int[] mInts = { 1, 2 };
        private Integer[] mIntegers = { 1, 2 };
        private String[] mStrings = { "1", "2" };
        private Character[] mCharacters = { '1', '2' };
        private char[] mChars = { '1', '2' };
        private Boolean[] mBooleans = { true, false };
        private boolean[] mBooleanValues = { true, false };
        private Long[] mLongs = { 1L, 2L };
        private long[] mLongValues = { 1L, 2L };
        private Double[] mDoubles = { 1.0, 2.0 };
        private double[] mDoubleValues = { 1.0, 2.0 };
        private Float[] mFloats = { 1.0f, 2.0f };
        private float[] mFloatValues = { 1.0f, 2.0f };
        private BigDecimal[] mBigDecimals = { BigDecimal.ONE, BigDecimal.TEN };
        private BigInteger[] mBigIntegers = { BigInteger.ONE, BigInteger.TEN };

        private List<Integer> mIntegerList = Collections.singletonList(1);
        private List<String> mStringList = Collections.singletonList("1");
        private List<Character> mCharacterList = Collections.singletonList('1');
        private List<Boolean> mBooleanList = Collections.singletonList(true);
        private List<Long> mLongList = Collections.singletonList(1L);
        private List<Double> mDoubleList = Collections.singletonList(1.0);
        private List<Float> mFloatList = Collections.singletonList(1.0f);
        private List<BigDecimal> mBigDecimalList = Collections.singletonList(BigDecimal.ONE);
        private List<BigInteger> mBigIntegerList = Collections.singletonList(BigInteger.ONE);

        private Map<Integer, Integer> mIntegerMap = singletonMap(1, 1);
        private Map<Integer, String> mStringMap = singletonMap(2, "1");
        private Map<Integer, Character> mCharacterMap = singletonMap(3, '1');
        private Map<Integer, Boolean> mBooleanLMap = singletonMap(4, true);
        private Map<Integer, Long> mLongMap = singletonMap(5, 1L);
        private Map<Integer, Double> mDoubleMap = singletonMap(6, 1.0);
        private Map<Integer, Float> mFloatMap = singletonMap(7, 1.0f);
        private Map<Integer, BigDecimal> mBigDecimalMap = singletonMap(8, BigDecimal.ONE);
        private Map<Integer, BigInteger> mBigIntegerMap = singletonMap(9, BigInteger.ONE);

        private SourceChild child = new SourceChild();
        private SourceChild[] childArray = { new SourceChild(), new SourceChild() };
        private List<SourceChild> childList = Collections.singletonList(new SourceChild());
        private Map<Integer, SourceChild> childMap = singletonMap(1, new SourceChild());
        private Map<SourceChild, Integer> childRMap = singletonMap(new SourceChild(), 1);
    }

    @Data
    public static class SourceChild {
        private Integer mInteger = 2;
        private int mInt = 2;
        private String mString = "2";
        private Character mCharacter = '2';
        private char mChar = '2';
        private Boolean mBoolean = true;
        private boolean mBooleanValue = true;
        private Long mLong = 2L;
        private long mLongValue = 2L;
        private Double mDouble = 2.0;
        private double mDoubleValue = 2.0;
        private Float mFloat = 2.0f;
        private float mFloatValue = 2.0f;
        private BigDecimal mBigDecimal = BigDecimal.ONE.add(BigDecimal.ONE);
        private BigInteger mBigInteger = BigInteger.ONE.add(BigInteger.ONE);

        private int[] mInts = { 3, 4 };
        private Integer[] mIntegers = { 3, 4 };
        private String[] mStrings = { "3", "4" };
        private Character[] mCharacters = { '3', '4' };
        private char[] mChars = { '3', '4' };
        private Boolean[] mBooleans = { true, false };
        private boolean[] mBooleanValues = { true, false };
        private Long[] mLongs = { 3L, 4L };
        private long[] mLongValues = { 3L, 4L };
        private Double[] mDoubles = { 3.0, 4.0 };
        private double[] mDoubleValues = { 3.0, 4.0 };
        private Float[] mFloats = { 3.0f, 4.0f };
        private float[] mFloatValues = { 3.0f, 4.0f };
        private BigDecimal[] mBigDecimals = { BigDecimal.ONE.add(BigDecimal.ONE), BigDecimal.TEN.add(BigDecimal.TEN) };
        private BigInteger[] mBigIntegers = { BigInteger.ONE.add(BigInteger.ONE), BigInteger.TEN.add(BigInteger.TEN) };

        private List<Integer> mIntegerList = Collections.singletonList(2);
        private List<String> mStringList = Collections.singletonList("2");
        private List<Character> mCharacterList = Collections.singletonList('2');
        private List<Boolean> mBooleanList = Collections.singletonList(true);
        private List<Long> mLongList = Collections.singletonList(2L);
        private List<Double> mDoubleList = Collections.singletonList(2.0);
        private List<Float> mFloatList = Collections.singletonList(2.0f);
        private List<BigDecimal> mBigDecimalList = Collections.singletonList(BigDecimal.ONE.add(BigDecimal.ONE));
        private List<BigInteger> mBigIntegerList = Collections.singletonList(BigInteger.ONE.add(BigInteger.ONE));

        private Map<Integer, Integer> mIntegerMap = singletonMap(10, 2);
        private Map<Integer, String> mStringMap = singletonMap(20, "2");
        private Map<Integer, Character> mCharacterMap = singletonMap(30, '2');
        private Map<Integer, Boolean> mBooleanLMap = singletonMap(40, true);
        private Map<Integer, Long> mLongMap = singletonMap(50, 2L);
        private Map<Integer, Double> mDoubleMap = singletonMap(60, 2.0);
        private Map<Integer, Float> mFloatMap = singletonMap(70, 2.0f);
        private Map<Integer, BigDecimal> mBigDecimalMap = singletonMap(80, BigDecimal.ONE.add(BigDecimal.ONE));
        private Map<Integer, BigInteger> mBigIntegerMap = singletonMap(90, BigInteger.ONE.add(BigInteger.ONE));
    }

    private static <K, V> Map<K, V> singletonMap(K key, V value) {
        HashMap<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @Data
    public static class Target {
        private Integer mInteger;
        private int mInt;
        private String mString;
        private Character mCharacter;
        private char mChar;
        private Boolean mBoolean;
        private boolean mBooleanValue;
        private Long mLong;
        private long mLongValue;
        private Double mDouble;
        private double mDoubleValue;
        private Float mFloat;
        private float mFloatValue;
        private BigDecimal mBigDecimal;
        private BigInteger mBigInteger;

        private int[] mInts;
        private Integer[] mIntegers;
        private String[] mStrings;
        private Character[] mCharacters;
        private char[] mChars;
        private Boolean[] mBooleans;
        private boolean[] mBooleanValues;
        private Long[] mLongs;
        private long[] mLongValues;
        private Double[] mDoubles;
        private double[] mDoubleValues;
        private Float[] mFloats;
        private float[] mFloatValues;
        private BigDecimal[] mBigDecimals;
        private BigInteger[] mBigIntegers;

        private List<Integer> mIntegerList;
        private List<String> mStringList;
        private List<Character> mCharacterList;
        private List<Boolean> mBooleanList;
        private List<Long> mLongList;
        private List<Double> mDoubleList;
        private List<Float> mFloatList;
        private List<BigDecimal> mBigDecimalList;
        private List<BigInteger> mBigIntegerList;

        private Map<Integer, Integer> mIntegerMap;
        private Map<Integer, String> mStringMap;
        private Map<Integer, Character> mCharacterMap;
        private Map<Integer, Boolean> mBooleanLMap;
        private Map<Integer, Long> mLongMap;
        private Map<Integer, Double> mDoubleMap;
        private Map<Integer, Float> mFloatMap ;
        private Map<Integer, BigDecimal> mBigDecimalMap;
        private Map<Integer, BigInteger> mBigIntegerMap;

        private TargetChild child;
        private TargetChild[] childArray;
        private List<TargetChild> childList;
        private Map<Integer, TargetChild> childMap;
        private Map<TargetChild, Integer> childRMap;
    }

    @Data
    public static class TargetChild {
        private Integer mInteger;
        private int mInt;
        private String mString;
        private Character mCharacter;
        private char mChar;
        private Boolean mBoolean;
        private boolean mBooleanValue;
        private Long mLong;
        private long mLongValue;
        private Double mDouble;
        private double mDoubleValue;
        private Float mFloat;
        private float mFloatValue;
        private BigDecimal mBigDecimal;
        private BigInteger mBigInteger;

        private int[] mInts;
        private Integer[] mIntegers;
        private String[] mStrings;
        private Character[] mCharacters;
        private char[] mChars;
        private Boolean[] mBooleans;
        private boolean[] mBooleanValues;
        private Long[] mLongs;
        private long[] mLongValues;
        private Double[] mDoubles;
        private double[] mDoubleValues;
        private Float[] mFloats;
        private float[] mFloatValues;
        private BigDecimal[] mBigDecimals;
        private BigInteger[] mBigIntegers;

        private List<Integer> mIntegerList;
        private List<String> mStringList;
        private List<Character> mCharacterList;
        private List<Boolean> mBooleanList;
        private List<Long> mLongList;
        private List<Double> mDoubleList;
        private List<Float> mFloatList;
        private List<BigDecimal> mBigDecimalList;
        private List<BigInteger> mBigIntegerList;

        private Map<Integer, Integer> mIntegerMap;
        private Map<Integer, String> mStringMap;
        private Map<Integer, Character> mCharacterMap;
        private Map<Integer, Boolean> mBooleanLMap;
        private Map<Integer, Long> mLongMap;
        private Map<Integer, Double> mDoubleMap;
        private Map<Integer, Float> mFloatMap ;
        private Map<Integer, BigDecimal> mBigDecimalMap;
        private Map<Integer, BigInteger> mBigIntegerMap;
    }

    @Test
    public void copy() {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "/Users/lpzahd/IdeaProjects/cglib-copier/cglib");
        Source source = new Source();
        Target target = Cglib.copyByClass(source, Target.class);
        System.out.println(target);

//        CopyTest.Source var5 = (CopyTest.Source)source;
//        CopyTest.Target var6 = (CopyTest.Target)target;
//        var6.setMLongValue(var5.getMLongValue());
//        System.out.println(var6);

//        CopyTest.Source var5 = (CopyTest.Source)source;
//        CopyTest.Target var6 = (CopyTest.Target)target;
//        Map var7 = (Map)var5.getChildRMap();
//        if (var7 != null) {
//            TreeMap<CopyTest.Target, Integer> var11 = new TreeMap<>();
//            Set var8 = var7.entrySet();
//            Iterator var9 = var8.iterator();
//
//            while(var9.hasNext()) {
//                Map.Entry var10 = (Map.Entry)var9.next();
//                CopyTest.SourceChild var12 = (CopyTest.SourceChild)var10.getKey();
//                Integer var13 = (Integer)var10.getValue();
//                var11.put(var12, var13);
//            }
//
//            var6.setChildRMap(var11);
//        }
    }
}
