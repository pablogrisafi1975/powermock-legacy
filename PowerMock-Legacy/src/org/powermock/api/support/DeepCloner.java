/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powermock.api.support;

import java.lang.reflect.Field;

import org.powermock.reflect.Whitebox;

/**
 * The purpose of the deep cloner is to create a deep clone of an object.
 * Classes
 */
public class DeepCloner {
    private static final String IGNORED_PACKAGES = "java.";

    /**
     * Clones an object.
     * 
     * @return A deep clone of the object to clone.
     */
    public static <T> T clone(T objectToClone) {
        assertObjectNotNull(objectToClone);
        return (T) performClone(getType(objectToClone), objectToClone);
    }

    /**
     * Clone an object into an object loaded by the supplied classloader.
     */
    public static <T> T clone(ClassLoader classloader, T objectToClone) {
        assertObjectNotNull(objectToClone);
        return performClone(ClassLoaderUtil.loadClassWithClassloader(classloader, getType(objectToClone)), objectToClone);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getType(T objectToClone) {
        return (Class<T>) (objectToClone instanceof Class ? objectToClone : objectToClone.getClass());
    }

    private static void assertObjectNotNull(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object to clone cannot be null");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T performClone(Class<T> targetClass, Object source) {
        Object target = Whitebox.newInstance(targetClass);
        Class<?> currentTargetClass = targetClass;
        while (currentTargetClass != null && !currentTargetClass.getName().startsWith(IGNORED_PACKAGES)) {
            for (Field field : currentTargetClass.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    final Field declaredField = source.getClass().getDeclaredField(field.getName());
                    declaredField.setAccessible(true);
                    final Object object = declaredField.get(source);
                    final Object instantiatedValue;
                    if (Whitebox.getType(object).getName().startsWith(IGNORED_PACKAGES) || object == null) {
                        instantiatedValue = object;
                    } else {
                        instantiatedValue = performClone(ClassLoaderUtil.loadClassWithClassloader(targetClass.getClassLoader(), getType(object)),
                                object);
                    }
                    field.set(target, instantiatedValue);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            currentTargetClass = currentTargetClass.getSuperclass();
        }
        return (T) target;
    }
}
