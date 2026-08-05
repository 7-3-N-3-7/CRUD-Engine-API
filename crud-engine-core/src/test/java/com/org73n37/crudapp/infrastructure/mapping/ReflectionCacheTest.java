package com.org73n37.crudapp.infrastructure.mapping;

import static org.junit.jupiter.api.Assertions.*;

import com.org73n37.crudapp.infrastructure.annotations.Parent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ReflectionCacheTest {

    static class SampleClass {
        @Parent
        private String parentField;
        private int otherField;

        public SampleClass() {}
    }

    static class MissingNoArgsConstructor {
        private String name;
        public MissingNoArgsConstructor(String name) {
            this.name = name;
        }
    }

    @Test
    void testGetDeclaredFieldsAndCaching() {
        Field[] fields1 = ReflectionCache.getDeclaredFields(SampleClass.class);
        Field[] fields2 = ReflectionCache.getDeclaredFields(SampleClass.class);

        assertSame(fields1, fields2, "ReflectionCache should return the exact same array instance due to caching");
        assertEquals(2, fields1.length, "SampleClass should have 2 declared fields");
    }

    @Test
    void testGetNoArgsConstructorSuccess() throws Exception {
        Constructor<SampleClass> ctor1 = ReflectionCache.getNoArgsConstructor(SampleClass.class);
        Constructor<SampleClass> ctor2 = ReflectionCache.getNoArgsConstructor(SampleClass.class);

        assertSame(ctor1, ctor2, "ReflectionCache should cache the constructor instance");
        assertNotNull(ctor1.newInstance(), "Should be able to instantiate using resolved constructor");
    }

    @Test
    void testGetNoArgsConstructorMissingThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionCache.getNoArgsConstructor(MissingNoArgsConstructor.class);
        }, "Should throw IllegalArgumentException when no-args constructor is missing");
    }

    @Test
    void testGetAnnotationsAndSpecificAnnotation() throws Exception {
        Field field = SampleClass.class.getDeclaredField("parentField");

        Parent parentAnn1 = ReflectionCache.getAnnotation(field, Parent.class);
        Parent parentAnn2 = ReflectionCache.getAnnotation(field, Parent.class);

        assertNotNull(parentAnn1, "Should find the @Parent annotation on the field");
        assertSame(parentAnn1, parentAnn2, "Annotation lookup should be cached and return same reference");

        Field nonAnnotatedField = SampleClass.class.getDeclaredField("otherField");
        assertNull(ReflectionCache.getAnnotation(nonAnnotatedField, Parent.class), "Should return null for non-existent annotation");
    }
}
