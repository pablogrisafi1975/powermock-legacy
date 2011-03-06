/*
 * Copyright 2008 the original author or authors.
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
package org.powermock.reflect.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import org.powermock.reflect.exceptions.ConstructorNotFoundException;
import org.powermock.reflect.exceptions.FieldNotFoundException;
import org.powermock.reflect.exceptions.MethodInvocationException;
import org.powermock.reflect.exceptions.MethodNotFoundException;
import org.powermock.reflect.exceptions.TooManyConstructorsFoundException;
import org.powermock.reflect.exceptions.TooManyFieldsFoundException;
import org.powermock.reflect.exceptions.TooManyMethodsFoundException;
import org.powermock.reflect.internal.matcherstrategies.AllFieldsMatcherStrategy;
import org.powermock.reflect.internal.matcherstrategies.AssignableFromFieldTypeMatcherStrategy;
import org.powermock.reflect.internal.matcherstrategies.FieldAnnotationMatcherStrategy;
import org.powermock.reflect.internal.matcherstrategies.FieldMatcherStrategy;
import org.powermock.reflect.internal.matcherstrategies.FieldNameMatcherStrategy;
import org.powermock.reflect.internal.primitivesupport.PrimitiveWrapper;
import org.powermock.reflect.spi.ProxyFramework;

/**
 * Various utilities for accessing internals of a class. Basically a simplified
 * reflection utility intended for tests.
 */
public class WhiteboxImpl {

	private static ProxyFramework proxyFramework = null;

	/**
	 * Convenience method to get a method from a class type without having to
	 * catch the checked exceptions otherwise required. These exceptions are
	 * wrapped as runtime exceptions.
	 * <p>
	 * The method will first try to look for a declared method in the same
	 * class. If the method is not declared in this class it will look for the
	 * method in the super class. This will continue throughout the whole class
	 * hierarchy. If the method is not found an {@link MethodNotFoundException}
	 * is thrown. Since the method name is not specified an
	 * {@link TooManyMethodsFoundException} is thrown if two or more methods
	 * matches the same parameter types in the same class.
	 * 
	 * @param type
	 *            The type of the class where the method is located.
	 * @param parameterTypes
	 *            All parameter types of the method (may be <code>null</code>).
	 * @return A <code>java.lang.reflect.Method</code>.
	 * @throws MethodNotFoundException
	 *             If a method cannot be found in the hierarchy.
	 * @throws TooManyMethodsFoundException
	 *             If several methods were found.
	 */
	public static Method getMethod(Class<?> type, Class<?>... parameterTypes) {
		Class<?> thisType = type;
		if (parameterTypes == null) {
			parameterTypes = new Class<?>[0];
		}

		List<Method> foundMethods = new LinkedList<Method>();
		while (thisType != null) {
			Method[] methodsToTraverse = null;
			if (thisType.isInterface()) {
				// Interfaces only contain public (and abstract) methods, no
				// need to traverse the hierarchy.
				methodsToTraverse = getAllPublicMethods(thisType);
			} else {
				methodsToTraverse = thisType.getDeclaredMethods();
			}
			for (Method method : methodsToTraverse) {
				if (checkIfTypesAreSame(parameterTypes, method.getParameterTypes())) {
					foundMethods.add(method);
					if (foundMethods.size() == 1) {
						method.setAccessible(true);
					}
				}

			}
			if (foundMethods.size() == 1) {
				return foundMethods.get(0);
			} else if (foundMethods.size() > 1) {
				break;
			}
			thisType = thisType.getSuperclass();
		}

		if (foundMethods.isEmpty()) {
			throw new MethodNotFoundException("No method was found with parameter types: [ " + getArgumentTypesAsString((Object[]) parameterTypes)
					+ " ] in class " + getUnmockedType(type).getName() + ".");
		} else {
			throwExceptionWhenMultipleMethodMatchesFound("method name", foundMethods.toArray(new Method[foundMethods.size()]));
		}
		// Will never happen
		return null;
	}

	/**
	 * Convenience method to get a method from a class type without having to
	 * catch the checked exceptions otherwise required. These exceptions are
	 * wrapped as runtime exceptions.
	 * <p>
	 * The method will first try to look for a declared method in the same
	 * class. If the method is not declared in this class it will look for the
	 * method in the super class. This will continue throughout the whole class
	 * hierarchy. If the method is not found an {@link IllegalArgumentException}
	 * is thrown.
	 * 
	 * @param type
	 *            The type of the class where the method is located.
	 * @param methodName
	 *            The method names.
	 * @param parameterTypes
	 *            All parameter types of the method (may be <code>null</code>).
	 * @return A <code>java.lang.reflect.Method</code>.
	 * @throws MethodNotFoundException
	 *             If a method cannot be found in the hierarchy.
	 */
	public static Method getMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Class<?> thisType = type;
		if (parameterTypes == null) {
			parameterTypes = new Class<?>[0];
		}
		while (thisType != null) {
			Method[] methodsToTraverse = null;
			if (thisType.isInterface()) {
				// Interfaces only contain public (and abstract) methods, no
				// need to traverse the hierarchy.
				methodsToTraverse = getAllPublicMethods(thisType);
			} else {
				methodsToTraverse = thisType.getDeclaredMethods();
			}
			for (Method method : methodsToTraverse) {
				if (methodName.equals(method.getName()) && checkIfTypesAreSame(parameterTypes, method.getParameterTypes())) {
					method.setAccessible(true);
					return method;
				}
			}
			thisType = thisType.getSuperclass();
		}

		throwExceptionIfMethodWasNotFound(type, methodName, null, new Object[] { parameterTypes });
		return null;
	}

	/**
	 * Convenience method to get a field from a class type.
	 * <p>
	 * The method will first try to look for a declared field in the same class.
	 * If the method is not declared in this class it will look for the field in
	 * the super class. This will continue throughout the whole class hierarchy.
	 * If the field is not found an {@link IllegalArgumentException} is thrown.
	 * 
	 * @param type
	 *            The type of the class where the method is located.
	 * @param fieldName
	 *            The method names.
	 * @return A <code>java.lang.reflect.Field</code>.
	 * @throws FieldNotFoundException
	 *             If a field cannot be found in the hierarchy.
	 */
	@SuppressWarnings("unchecked")
	public static Field getField(Class<?> type, String fieldName) {
		LinkedList<Class<?>> examine = new LinkedList<Class<?>>();
		examine.add(type);
		Set<Class<?>> done = new HashSet<Class<?>>();
		while (examine.isEmpty() == false) {
			Class<?> thisType = examine.removeFirst();
			done.add(thisType);
			final Field[] declaredField = thisType.getDeclaredFields();
			for (Field field : declaredField) {
				if (fieldName.equals(field.getName())) {
					field.setAccessible(true);
					return field;
				}
			}
			Set<Class<?>> potential = new HashSet<Class<?>>();
			final Class<?> clazz = thisType.getSuperclass();
			if (clazz != null) {
				potential.add(thisType.getSuperclass());
			}
			potential.addAll((Collection) Arrays.asList(thisType.getInterfaces()));
			potential.removeAll(done);
			examine.addAll(potential);
		}

		throwExceptionIfFieldWasNotFound(type, fieldName, null);
		return null;
	}

	/**
	 * Create a new instance of a class without invoking its constructor.
	 * <p>
	 * No byte-code manipulation is needed to perform this operation and thus
	 * it's not necessary use the <code>PowerMockRunner</code> or
	 * <code>PrepareForTest</code> annotation to use this functionality.
	 * 
	 * @param <T>
	 *            The type of the instance to create.
	 * @param classToInstantiate
	 *            The type of the instance to create.
	 * @return A new instance of type T, created without invoking the
	 *         constructor.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> classToInstantiate) {
		Objenesis objenesis = new ObjenesisStd();
		ObjectInstantiator thingyInstantiator = objenesis.getInstantiatorOf(classToInstantiate);
		return (T) thingyInstantiator.newInstance();
	}

	/**
	 * Convenience method to get a (declared) constructor from a class type
	 * without having to catch the checked exceptions otherwise required. These
	 * exceptions are wrapped as runtime exceptions. The constructor is also set
	 * to accessible.
	 * 
	 * @param type
	 *            The type of the class where the constructor is located.
	 * @param parameterTypes
	 *            All parameter types of the constructor (may be
	 *            <code>null</code>).
	 * @return A <code>java.lang.reflect.Constructor</code>.
	 * @throws ConstructorNotFoundException
	 *             if the constructor cannot be found.
	 */
	public static Constructor<?> getConstructor(Class<?> type, Class<?>... parameterTypes) {
		Class<?> unmockedType = WhiteboxImpl.getUnmockedType(type);
		try {
			final Constructor<?> constructor = unmockedType.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			return constructor;
		} catch (RuntimeException e) {
			throw (RuntimeException) e;
		} catch (Error e) {
			throw (Error) e;
		} catch (Throwable e) {
			throw new ConstructorNotFoundException(String.format("Failed to lookup constructor with parameter types [ %s ] in class %s.",
					getArgumentTypesAsString((Object[]) parameterTypes), unmockedType.getName()), e);
		}
	}

	/**
	 * Set the value of a field using reflection. This method will traverse the
	 * super class hierarchy until a field with name <tt>fieldName</tt> is
	 * found.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param value
	 *            the new value of the field
	 */
	public static void setInternalState(Object object, String fieldName, Object value) {
		Field foundField = findFieldInHierarchy(object, fieldName);
		setField(object, value, foundField);
	}

	/**
	 * Set the value of a field using reflection. This method will traverse the
	 * super class hierarchy until the first field of type <tt>fieldType</tt> is
	 * found. The <tt>value</tt> will then be assigned to this field.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldType
	 *            the type of the field
	 * @param value
	 *            the new value of the field
	 */
	public static void setInternalState(Object object, Class<?> fieldType, Object value) {
		setField(object, value, findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(fieldType)));
	}

	/**
	 * Set the value of a field using reflection. This method will traverse the
	 * super class hierarchy until the first field assignable to the
	 * <tt>value</tt> type is found. The <tt>value</tt> (or
	 * <tt>additionaValues</tt> if present) will then be assigned to this field.
	 * 
	 * @param object
	 *            the object to modify
	 * @param value
	 *            the new value of the field
	 * @param additionalValues
	 *            Additional values to set on the object
	 */
	public static void setInternalState(Object object, Object value, Object... additionalValues) {
		setField(object, value, findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(getType(value))));
		if (additionalValues != null && additionalValues.length > 0) {
			for (Object additionalValue : additionalValues) {
				setField(object, additionalValue, findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(getType(additionalValue))));
			}
		}
	}

	/**
	 * Set the value of a field using reflection at at specific place in the
	 * class hierarchy (<tt>where</tt>). This first field assignable to
	 * <tt>object</tt> will then be set to <tt>value</tt>.
	 * 
	 * @param object
	 *            the object to modify
	 * @param value
	 *            the new value of the field
	 * @param where
	 *            the class in the hierarchy where the field is defined
	 */
	public static void setInternalState(Object object, Object value, Class<?> where) {
		setField(object, value, findField(object, new AssignableFromFieldTypeMatcherStrategy(getType(value)), where));
	}

	/**
	 * Set the value of a field using reflection at a specific location (
	 * <tt>where</tt>) in the class hierarchy. The <tt>value</tt> will then be
	 * assigned to this field.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldType
	 *            the type of the field the should be set.
	 * @param value
	 *            the new value of the field
	 * @param where
	 *            which class in the hierarchy defining the field
	 */
	public static void setInternalState(Object object, Class<?> fieldType, Object value, Class<?> where) {
		if (fieldType == null || where == null) {
			throw new IllegalArgumentException("fieldType and where cannot be null");
		}

		setField(object, value, findFieldOrThrowException(fieldType, where));
	}

	/**
	 * Set the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. This is useful if you
	 * have two fields in a class hierarchy that has the same name but you like
	 * to modify the latter.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param value
	 *            the new value of the field
	 * @param where
	 *            which class the field is defined
	 */
	public static void setInternalState(Object object, String fieldName, Object value, Class<?> where) {
		if (object == null || fieldName == null || fieldName.equals("") || fieldName.startsWith(" ")) {
			throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
		}

		final Field field = getField(fieldName, where);
		try {
			field.set(object, value);
		} catch (Exception e) {
			throw new RuntimeException("Internal Error: Failed to set field in method setInternalState.", e);
		}
	}

	/**
	 * Get the value of a field using reflection. This method will iterate
	 * through the entire class hierarchy and return the value of the first
	 * field named <tt>fieldName</tt>. If you want to get a specific field value
	 * at specific place in the class hierarchy please refer to
	 * {@link #getInternalState(Object, String, Class)}.
	 * 
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object object, String fieldName) {
		Field foundField = findFieldInHierarchy(object, fieldName);
		try {
			return (T) foundField.get(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
		}
	}

	private static Field findFieldInHierarchy(Object object, String fieldName) {
		return findFieldInHierarchy(object, new FieldNameMatcherStrategy(fieldName));
	}

	private static Field findFieldInHierarchy(Object object, FieldMatcherStrategy strategy) {
		return findSingleFieldUsingStrategy(strategy, object, true, getType(object));
	}

	private static Field findField(Object object, FieldMatcherStrategy strategy, Class<?> where) {
		return findSingleFieldUsingStrategy(strategy, object, false, where);
	}

	private static Field findSingleFieldUsingStrategy(FieldMatcherStrategy strategy, Object object, boolean checkHierarchy, Class<?> startClass) {
		if (object == null) {
			throw new IllegalArgumentException("The object containing the field cannot be null");
		}
		Field foundField = null;
		final Class<?> originalStartClass = startClass;
		while (startClass != null) {
			final Field[] declaredFields = startClass.getDeclaredFields();
			for (Field field : declaredFields) {
				if (strategy.matches(field) && hasFieldProperModifier(object, field)) {
					if (foundField != null) {
						throw new TooManyFieldsFoundException("Two or more fields matching " + strategy + ".");
					}
					foundField = field;
				}
			}
			if (foundField != null) {
				break;
			} else if (checkHierarchy == false) {
				break;
			}
			startClass = startClass.getSuperclass();
		}
		if (foundField == null) {
			strategy.notFound(originalStartClass, !isClass(object));
		}
		foundField.setAccessible(true);
		return foundField;
	}

	private static Set<Field> findAllFieldsUsingStrategy(FieldMatcherStrategy strategy, Object object, boolean checkHierarchy, Class<?> startClass) {
		if (object == null) {
			throw new IllegalArgumentException("The object containing the field cannot be null");
		}
		final Set<Field> foundFields = new LinkedHashSet<Field>();
		while (startClass != null) {
			final Field[] declaredFields = startClass.getDeclaredFields();
			for (Field field : declaredFields) {
				if (strategy.matches(field) && hasFieldProperModifier(object, field)) {
					field.setAccessible(true);
					foundFields.add(field);
				}
			}
			if (!checkHierarchy) {
				break;
			}
			startClass = startClass.getSuperclass();
		}

		return Collections.unmodifiableSet(foundFields);
	}

	private static boolean hasFieldProperModifier(Object object, Field field) {
		return ((object instanceof Class<?> && Modifier.isStatic(field.getModifiers())) || ((object instanceof Class<?> == false && Modifier
				.isStatic(field.getModifiers()) == false)));
	}

	/**
	 * Get the value of a field using reflection. This method will traverse the
	 * super class hierarchy until the first field of type <tt>fieldType</tt> is
	 * found. The value of this field will be returned.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldType
	 *            the type of the field
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object object, Class<T> fieldType) {
		Field foundField = findFieldInHierarchy(object, new AssignableFromFieldTypeMatcherStrategy(fieldType));
		try {
			return (T) foundField.get(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
		}
	}

	/**
	 * Get the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. The first field matching
	 * the <tt>fieldType</tt> in <tt>where</tt> will is the field whose value
	 * will be returned.
	 * 
	 * @param <T>
	 *            the expected type of the field
	 * @param object
	 *            the object to modify
	 * @param fieldType
	 *            the type of the field
	 * @param where
	 *            which class the field is defined
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object object, Class<T> fieldType, Class<?> where) {
		if (object == null) {
			throw new IllegalArgumentException("object and type are not allowed to be null");
		}

		try {
			return (T) findFieldOrThrowException(fieldType, where).get(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
		}
	}

	/**
	 * Get the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. This might be useful
	 * when you have mocked the instance you are trying to access. Use this
	 * method to avoid casting.
	 * 
	 * @param <T>
	 *            the expected type of the field
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param where
	 *            which class the field is defined
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object object, String fieldName, Class<?> where) {
		if (object == null || fieldName == null || fieldName.equals("") || fieldName.startsWith(" ")) {
			throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
		}

		Field field = null;
		try {
			field = where.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch (NoSuchFieldException e) {
			throw new FieldNotFoundException("Field '" + fieldName + "' was not found in class " + where.getName() + ".");
		} catch (Exception e) {
			throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
		}
	}

	/**
	 * Invoke a private or inner class method without the need to specify the
	 * method name. This is thus a more refactor friendly version of the
	 * {@link #invokeMethod(Object, String, Object...)} method and is recommend
	 * over this method for that reason. This method might be useful to test
	 * private methods.
	 * 
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object tested, Object... arguments) throws Exception {
		return (T) doInvokeMethod(tested, null, null, arguments);
	}

	/**
	 * Invoke a private or inner class method without the need to specify the
	 * method name. This is thus a more refactor friendly version of the
	 * {@link #invokeMethod(Object, String, Object...)} method and is recommend
	 * over this method for that reason. This method might be useful to test
	 * private methods.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Class<?> tested, Object... arguments) throws Exception {
		return (T) doInvokeMethod(tested, null, null, arguments);
	}

	/**
	 * Invoke a private or inner class method. This might be useful to test
	 * private methods.
	 * 
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object tested, String methodToExecute, Object... arguments) throws Exception {
		return (T) doInvokeMethod(tested, null, methodToExecute, arguments);
	}

	/**
	 * Invoke a private or inner class method in cases where power mock cannot
	 * automatically determine the type of the parameters, for example when
	 * mixing primitive types and wrapper types in the same method. For most
	 * situations use {@link #invokeMethod(Class, String, Object...)} instead.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object tested, String methodToExecute, Class<?>[] argumentTypes, Object... arguments)
			throws Exception {
		final Class<?> unmockedType = getType(tested);
		Method method = getMethod(unmockedType, methodToExecute, argumentTypes);
		if (method == null) {
			throwExceptionIfMethodWasNotFound(unmockedType, methodToExecute, null, arguments);
		}
		return (T) performMethodInvocation(tested, method, arguments);
	}

	/**
	 * Invoke a private or inner class method in a subclass (defined by
	 * <code>definedIn</code>) in cases where power mock cannot automatically
	 * determine the type of the parameters, for example when mixing primitive
	 * types and wrapper types in the same method. For most situations use
	 * {@link #invokeMethod(Class, String, Object...)} instead.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object tested, String methodToExecute, Class<?> definedIn, Class<?>[] argumentTypes,
			Object... arguments) throws Exception {
		Method method = getMethod(definedIn, methodToExecute, argumentTypes);
		if (method == null) {
			throwExceptionIfMethodWasNotFound(definedIn, methodToExecute, null, arguments);
		}
		return (T) performMethodInvocation(tested, method, arguments);
	}

	/**
	 * Invoke a private or inner class method in that is located in a subclass
	 * of the tested instance. This might be useful to test private methods.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object tested, Class<?> declaringClass, String methodToExecute, Object... arguments)
			throws Exception {
		return (T) doInvokeMethod(tested, declaringClass, methodToExecute, arguments);
	}

	/**
	 * Invoke a private method in that is located in a subclass of an instance.
	 * This might be useful to test overloaded private methods.
	 * <p>
	 * Use this for overloaded methods only, if possible use
	 * {@link #invokeMethod(Object, Object...)} or
	 * {@link #invokeMethod(Object, String, Object...)} instead.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Object object, Class<?> declaringClass, String methodToExecute, Class<?>[] parameterTypes,
			Object... arguments) throws Exception {
		if (object == null) {
			throw new IllegalArgumentException("object cannot be null");
		}

		final Method methodToInvoke = getMethod(declaringClass, methodToExecute, parameterTypes);
		// Invoke method
		return (T) performMethodInvocation(object, methodToInvoke, arguments);
	}

	/**
	 * Invoke a private or inner class method. This might be useful to test
	 * private methods.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> T invokeMethod(Class<?> clazz, String methodToExecute, Object... arguments) throws Exception {
		return (T) doInvokeMethod(clazz, null, methodToExecute, arguments);
	}

	@SuppressWarnings("unchecked")
	private static <T> T doInvokeMethod(Object tested, Class<?> declaringClass, String methodToExecute, Object... arguments) throws Exception {
		Method methodToInvoke = findMethodOrThrowException(tested, declaringClass, methodToExecute, arguments);

		// Invoke test
		return (T) performMethodInvocation(tested, methodToInvoke, arguments);
	}

	/**
	 * Finds and returns a certain method. If the method couldn't be found this
	 * method delegates to
	 * {@link WhiteboxImpl#throwExceptionIfMethodWasNotFound(Object, String, Method, Object...)}
	 * .
	 * 
	 * @param tested
	 *            The instance or class containing the method.
	 * @param declaringClass
	 *            The class where the method is supposed to be declared (may be
	 *            <code>null</code>).
	 * @param methodToExecute
	 *            The method name. If <code>null</code> then method will be
	 *            looked up based on the argument types only.
	 * @param arguments
	 *            The arguments of the methods.
	 * @return A single method.
	 */
	public static Method findMethodOrThrowException(Object tested, Class<?> declaringClass, String methodToExecute, Object[] arguments) {
		if (tested == null) {
			throw new IllegalArgumentException("The object to perform the operation on cannot be null.");
		}

		/*
		 * Get methods from the type if it's not mocked or from the super type
		 * if the tested object is mocked.
		 */
		Class<?> testedType = null;
		if (isClass(tested)) {
			testedType = (Class<?>) tested;
		} else {
			testedType = tested.getClass();
		}

		Method[] methods = null;
		if (declaringClass == null) {
			methods = getAllMethods(testedType);
		} else {
			methods = declaringClass.getDeclaredMethods();
		}
		Method potentialMethodToInvoke = null;
		for (Method method : methods) {
			if (methodToExecute == null || method.getName().equals(methodToExecute)) {
				Class<?>[] paramTypes = method.getParameterTypes();
				if ((arguments != null && (paramTypes.length == arguments.length))) {
					if (paramTypes.length == 0) {
						potentialMethodToInvoke = method;
						break;
					}
					boolean wrappedMethodFound = true;
					boolean primitiveMethodFound = true;
					if (!checkIfTypesAreSame(paramTypes, arguments)) {
						wrappedMethodFound = false;
					}

					if (!checkIfTypesAreSame(paramTypes, convertArgumentTypesToPrimitive(paramTypes, arguments))) {
						primitiveMethodFound = false;
					}

					if (wrappedMethodFound || primitiveMethodFound) {
						if (potentialMethodToInvoke == null) {
							potentialMethodToInvoke = method;
						} else {
							/*
							 * We've already found a method match before, this
							 * means that PowerMock cannot determine which
							 * method to expect since there are two methods with
							 * the same name and the same number of arguments
							 * but one is using wrapper types.
							 */
							throwExceptionWhenMultipleMethodMatchesFound("argument parameter types", new Method[] { potentialMethodToInvoke, method });
						}
					}
				} else if (isPotentialVarArgsMethod(method, arguments)) {
					if (potentialMethodToInvoke == null) {
						potentialMethodToInvoke = method;
					} else {
						/*
						 * We've already found a method match before, this means
						 * that PowerMock cannot determine which method to
						 * expect since there are two methods with the same name
						 * and the same number of arguments but one is using
						 * wrapper types.
						 */
						throwExceptionWhenMultipleMethodMatchesFound("argument parameter types", new Method[] { potentialMethodToInvoke, method });
					}
					break;
				} else if (arguments != null && (paramTypes.length != arguments.length)) {
					continue;
				}
			}
		}

		WhiteboxImpl.throwExceptionIfMethodWasNotFound(getType(tested), methodToExecute, potentialMethodToInvoke, arguments);
		return potentialMethodToInvoke;
	}

/**
	 * Finds and returns a certain constructor. If the constructor couldn't be
	 * found this method delegates to
	 * {@link Whitebox#throwExceptionIfConstructorWasNotFound(Class, Object...).
	 * 
	 * @param type The type where the constructor should be located. 
	 * @param arguments The arguments passed to the constructor. 
 	 * @return The found constructor.
 	 * @throws TooManyConstructorsFoundException If too many constructors matched.
 	 * @throws ConstructorNotFoundException If no constructor matched. 
	 */
	public static Constructor<?> findConstructorOrThrowException(Class<?> type) {
		final Constructor<?>[] declaredConstructors = filterPowerMockConstructor(type.getDeclaredConstructors());
		if (declaredConstructors.length > 1) {
			throwExceptionWhenMultipleConstructorMatchesFound(declaredConstructors);
		}
		return declaredConstructors[0];
	}

	private static Constructor<?>[] filterPowerMockConstructor(Constructor<?>[] declaredConstructors) {
		Set<Constructor<?>> constructors = new HashSet<Constructor<?>>();
		for (Constructor<?> constructor : declaredConstructors) {
			final Class<?>[] parameterTypes = constructor.getParameterTypes();
			if (parameterTypes.length == 1 && parameterTypes[0].getName().equals("org.powermock.core.IndicateReloadClass")) {
				continue;
			} else {
				constructors.add(constructor);
			}
		}
		return constructors.toArray(new Constructor<?>[constructors.size()]);
	}

/**
	 * Finds and returns a certain constructor. If the constructor couldn't be
	 * found this method delegates to
	 * {@link Whitebox#throwExceptionIfConstructorWasNotFound(Class, Object...).
	 * 
	 * @param type The type where the constructor should be located. 
	 * @param arguments The arguments passed to the constructor. 
 	 * @return The found constructor.
 	 * @throws TooManyConstructorsFoundException If too many constructors matched.
 	 * @throws ConstructorNotFoundException If no constructor matched. 
	 */
	public static Constructor<?> findUniqueConstructorOrThrowException(Class<?> type, Object... arguments) {
		if (type == null) {
			throw new IllegalArgumentException("Class type cannot be null.");
		}

		Class<?> unmockedType = getUnmockedType(type);
		if ((unmockedType.isLocalClass() || unmockedType.isAnonymousClass() || unmockedType.isMemberClass())
				&& !Modifier.isStatic(unmockedType.getModifiers()) && arguments != null) {
			Object[] argumentsForLocalClass = new Object[arguments.length + 1];
			argumentsForLocalClass[0] = unmockedType.getEnclosingClass();
			System.arraycopy(arguments, 0, argumentsForLocalClass, 1, arguments.length);
			arguments = argumentsForLocalClass;
		}

		Constructor<?>[] constructors = unmockedType.getDeclaredConstructors();
		Constructor<?> potentialConstructor = null;
		for (Constructor<?> constructor : constructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
			if ((arguments != null && (paramTypes.length == arguments.length))) {
				if (paramTypes.length == 0) {
					potentialConstructor = constructor;
					break;
				}
				boolean wrappedConstructorFound = true;
				boolean primitiveConstructorFound = true;
				if (!checkIfTypesAreSame(paramTypes, arguments)) {
					wrappedConstructorFound = false;
				}

				if (!checkIfTypesAreSame(paramTypes, convertArgumentTypesToPrimitive(paramTypes, arguments))) {
					primitiveConstructorFound = false;
				}

				if (wrappedConstructorFound || primitiveConstructorFound) {
					if (potentialConstructor == null) {
						potentialConstructor = constructor;
					} else {
						/*
						 * We've already found a constructor match before, this
						 * means that PowerMock cannot determine which method to
						 * expect since there are two methods with the same name
						 * and the same number of arguments but one is using
						 * wrapper types.
						 */
						throwExceptionWhenMultipleConstructorMatchesFound(new Constructor<?>[] { potentialConstructor, constructor });
					}
				}
			} else if (isPotentialVarArgsConstructor(constructor, arguments)) {
				if (potentialConstructor == null) {
					potentialConstructor = constructor;
				} else {
					/*
					 * We've already found a constructor match before, this
					 * means that PowerMock cannot determine which method to
					 * expect since there are two methods with the same name and
					 * the same number of arguments but one is using wrapper
					 * types.
					 */
					throwExceptionWhenMultipleConstructorMatchesFound(new Constructor<?>[] { potentialConstructor, constructor });
				}
				break;
			} else if (arguments != null && (paramTypes.length != arguments.length)) {
				continue;
			}
		}

		WhiteboxImpl.throwExceptionIfConstructorWasNotFound(type, potentialConstructor, arguments);
		return potentialConstructor;
	}

	private static Class<?>[] convertArgumentTypesToPrimitive(Class<?>[] paramTypes, Object[] arguments) {
		Class<?>[] types = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			Class<?> argumentType = null;
			if (arguments[i] == null) {
				argumentType = paramTypes[i];
			} else {
				argumentType = getType(arguments[i]);
			}
			Class<?> primitiveWrapperType = PrimitiveWrapper.getPrimitiveFromWrapperType(argumentType);
			if (primitiveWrapperType == null) {
				types[i] = argumentType;
			} else {
				types[i] = primitiveWrapperType;
			}
		}
		return types;
	}

	public static void throwExceptionIfMethodWasNotFound(Class<?> type, String methodName, Method methodToMock, Object... arguments) {
		if (methodToMock == null) {
			String methodNameData = "";
			if (methodName != null) {
				methodNameData = "with name '" + methodName + "' ";
			}
			throw new MethodNotFoundException("No method found " + methodNameData + "with parameter types: [ " + getArgumentTypesAsString(arguments)
					+ " ] in class " + getUnmockedType(type).getName() + ".");
		}
	}

	public static void throwExceptionIfFieldWasNotFound(Class<?> type, String fieldName, Field field) {
		if (field == null) {
			throw new FieldNotFoundException("No field was found with name '" + fieldName + "' in class " + getUnmockedType(type).getName() + ".");
		}
	}

	static void throwExceptionIfConstructorWasNotFound(Class<?> type, Constructor<?> potentialConstructor, Object... arguments) {
		if (potentialConstructor == null) {
			String message = "No constructor found in class '" + getUnmockedType(type).getName() + "' with " + "parameter types: [ "
					+ getArgumentTypesAsString(arguments) + " ].";
			throw new ConstructorNotFoundException(message);
		}
	}

	private static String getArgumentTypesAsString(Object... arguments) {
		StringBuilder argumentsAsString = new StringBuilder();
		final String noParameters = "<none>";
		if (arguments != null && arguments.length != 0) {
			for (int i = 0; i < arguments.length; i++) {
				String argumentName = null;
				Object argument = arguments[i];

				if (argument instanceof Class<?>) {
					argumentName = ((Class<?>) argument).getName();
				} else if (argument instanceof Class<?>[] && arguments.length == 1) {
					Class<?>[] argumentArray = (Class<?>[]) argument;
					if (argumentArray.length > 0) {
						for (int j = 0; j < argumentArray.length; j++) {
							appendArgument(argumentsAsString, j, argumentArray[j] == null ? "null" : getType(argumentArray[j]).getName(),
									argumentArray);
						}
						return argumentsAsString.toString();
					} else {
						argumentName = noParameters;
					}
				} else if (argument == null) {
					argumentName = "null";
				} else {
					argumentName = getType(argument).getName();
				}
				appendArgument(argumentsAsString, i, argumentName, arguments);
			}
		} else {
			argumentsAsString.append("<none>");
		}
		return argumentsAsString.toString();
	}

	private static void appendArgument(StringBuilder argumentsAsString, int index, String argumentName, Object[] arguments) {
		argumentsAsString.append(argumentName);
		if (index != arguments.length - 1) {
			argumentsAsString.append(", ");
		}
	}

	/**
	 * Invoke a constructor. Useful for testing classes with a private
	 * constructor when PowerMock cannot determine which constructor to invoke.
	 * This only happens if you have two constructors with the same number of
	 * arguments where one is using primitive data types and the other is using
	 * the wrapped counter part. For example:
	 * 
	 * <pre>
	 * public class MyClass {
	 *     private MyClass(Integer i) {
	 *         ...
	 *     } 
	 * 
	 *     private MyClass(int i) {
	 *         ...
	 *     }
	 * </pre>
	 * 
	 * This ought to be a really rare case. So for most situation, use
	 * {@link #invokeConstructor(Class, Object...)} instead.
	 * 
	 * 
	 * @return The object created after the constructor has been invoked.
	 * @throws Exception
	 *             If an exception occur when invoking the constructor.
	 */
	public static <T> T invokeConstructor(Class<T> classThatContainsTheConstructorToTest, Class<?>[] parameterTypes, Object[] arguments)
			throws Exception {
		if (parameterTypes != null && arguments != null) {
			if (parameterTypes.length != arguments.length) {
				throw new IllegalArgumentException("parameterTypes and arguments must have the same length");
			}
		}

		Constructor<T> constructor = null;
		try {
			constructor = classThatContainsTheConstructorToTest.getDeclaredConstructor(parameterTypes);
		} catch (Exception e) {
			throw new ConstructorNotFoundException("Could not lookup the constructor", e);
		}

		return createInstance(constructor, arguments);
	}

	/**
	 * Invoke a constructor. Useful for testing classes with a private
	 * constructor.
	 * 
	 * 
	 * @return The object created after the constructor has been invoked.
	 * @throws Exception
	 *             If an exception occur when invoking the constructor.
	 */
	public static <T> T invokeConstructor(Class<T> classThatContainsTheConstructorToTest, Object... arguments) throws Exception {

		if (classThatContainsTheConstructorToTest == null) {
			throw new IllegalArgumentException("The class should contain the constructor cannot be null.");
		}

		Class<?>[] argumentTypes = null;
		if (arguments == null) {
			argumentTypes = new Class<?>[0];
		} else {
			argumentTypes = new Class<?>[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				argumentTypes[i] = getType(arguments[i]);
			}
		}

		Constructor<T> constructor = null;

		Constructor<T> potentialContstructorWrapped = null;
		Constructor<T> potentialContstructorPrimitive = null;

		try {
			potentialContstructorWrapped = classThatContainsTheConstructorToTest.getDeclaredConstructor(argumentTypes);
		} catch (Exception e) {
			// Do nothing, we'll try with primitive type next.
		}

		try {
			potentialContstructorPrimitive = classThatContainsTheConstructorToTest.getDeclaredConstructor(PrimitiveWrapper
					.toPrimitiveType(argumentTypes));
		} catch (Exception e) {
			// Do nothing
		}

		if (potentialContstructorPrimitive == null && potentialContstructorWrapped == null) {
			// Check if we can find a matching var args constructor.
			constructor = getPotentialVarArgsConstructor(classThatContainsTheConstructorToTest, arguments);
			if (constructor == null) {
				throw new ConstructorNotFoundException("Failed to find a constructor with parameter types: [" + getArgumentTypesAsString(arguments)
						+ "]");
			}
		} else if (potentialContstructorPrimitive == null && potentialContstructorWrapped != null) {
			constructor = potentialContstructorWrapped;
		} else if (potentialContstructorPrimitive != null && potentialContstructorWrapped == null) {
			constructor = potentialContstructorPrimitive;
		} else if (arguments == null || arguments.length == 0 && potentialContstructorPrimitive != null) {
			constructor = potentialContstructorPrimitive;
		} else {
			throw new TooManyConstructorsFoundException(
					"Could not determine which constructor to execute. Please specify the parameter types by hand.");
		}

		return createInstance(constructor, arguments);
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getPotentialVarArgsConstructor(Class<T> classThatContainsTheConstructorToTest, Object... arguments) {
		if (areAllArgumentsOfSameType(arguments)) {
			Constructor<T>[] declaredConstructors = (Constructor<T>[]) classThatContainsTheConstructorToTest.getDeclaredConstructors();
			for (Constructor<T> possibleVarArgsConstructor : declaredConstructors) {
				if (possibleVarArgsConstructor.isVarArgs()) {
					if (arguments == null || arguments.length == 0) {
						return possibleVarArgsConstructor;
					} else {
						Class<?>[] parameterTypes = possibleVarArgsConstructor.getParameterTypes();
						if (parameterTypes[parameterTypes.length - 1].getComponentType().isAssignableFrom(getType(arguments[0]))) {
							return possibleVarArgsConstructor;
						}
					}
				}
			}
		}
		return null;
	}

	private static <T> T createInstance(Constructor<T> constructor, Object... arguments) throws Exception {
		if (constructor == null) {
			throw new IllegalArgumentException("Constructor cannot be null");
		}
		constructor.setAccessible(true);

		T createdObject = null;
		try {
			if (constructor.isVarArgs()) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				final int varArgsIndex = parameterTypes.length - 1;
				Class<?> varArgsType = parameterTypes[varArgsIndex].getComponentType();
				Object varArgsArrayInstance = createAndPopulateVarArgsArray(varArgsType, varArgsIndex, arguments);
				Object[] completeArgumentList = new Object[parameterTypes.length];
				for (int i = 0; i < varArgsIndex; i++) {
					completeArgumentList[i] = arguments[i];
				}
				completeArgumentList[completeArgumentList.length - 1] = varArgsArrayInstance;
				createdObject = constructor.newInstance(completeArgumentList);
			} else {
				createdObject = constructor.newInstance(arguments);
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			}
		}
		return createdObject;
	}

	private static Object createAndPopulateVarArgsArray(Class<?> varArgsType, int varArgsStartPosition, Object... arguments) {
		Object arrayInstance = Array.newInstance(varArgsType, arguments.length - varArgsStartPosition);
		for (int i = varArgsStartPosition; i < arguments.length; i++) {
			Array.set(arrayInstance, i - varArgsStartPosition, arguments[i]);
		}
		return arrayInstance;
	}

	/**
	 * Get all methods in a class hierarchy! Both declared an non-declared (no
	 * duplicates).
	 * 
	 * @param clazz
	 *            The class whose methods to get.
	 * @return All methods declared in this class hierarchy.
	 */
	public static Method[] getAllMethods(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("You must specify a class in order to get the methods.");
		}
		Set<Method> methods = new LinkedHashSet<Method>();

		Class<?> thisType = clazz;

		while (thisType != null) {
			final Class<?> type = thisType;
			final Method[] declaredMethods = AccessController.doPrivileged(new PrivilegedAction<Method[]>() {

				public Method[] run() {
					return type.getDeclaredMethods();
				}

			});
			for (Method method : declaredMethods) {
				method.setAccessible(true);
				methods.add(method);
			}
			thisType = thisType.getSuperclass();
		}
		return methods.toArray(new Method[0]);
	}

	/**
	 * Get all public methods for a class (no duplicates)! Note that the
	 * class-hierarchy will not be traversed.
	 * 
	 * @param clazz
	 *            The class whose methods to get.
	 * @return All public methods declared in <tt>this</tt> class.
	 */
	private static Method[] getAllPublicMethods(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("You must specify a class in order to get the methods.");
		}
		Set<Method> methods = new LinkedHashSet<Method>();

		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			methods.add(method);
		}
		return methods.toArray(new Method[0]);
	}

	/**
	 * Get all fields in a class hierarchy! Both declared an non-declared (no
	 * duplicates).
	 * 
	 * @param clazz
	 *            The class whose fields to get.
	 * @return All fields declared in this class hierarchy.
	 */
	public static Field[] getAllFields(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("You must specify the class that contains the fields");
		}
		Set<Field> fields = new LinkedHashSet<Field>();

		Class<?> thisType = clazz;

		while (thisType != null) {
			final Field[] declaredFields = thisType.getDeclaredFields();
			for (Field field : declaredFields) {
				field.setAccessible(true);
				fields.add(field);
			}
			thisType = thisType.getSuperclass();
		}
		return fields.toArray(new Field[fields.size()]);
	}

	/**
	 * Get the first parent constructor defined in a super class of
	 * <code>klass</code>.
	 * 
	 * @param klass
	 *            The class where the constructor is located. <code>null</code>
	 *            ).
	 * @return A <code>java.lang.reflect.Constructor</code>.
	 */
	public static Constructor<?> getFirstParentConstructor(Class<?> klass) {

		try {
			return getUnmockedType(klass).getSuperclass().getDeclaredConstructors()[0];
		} catch (Exception e) {
			throw new ConstructorNotFoundException("Failed to lookup constructor.", e);
		}
	}

	/**
	 * Finds and returns a method based on the input parameters. If no
	 * <code>parameterTypes</code> are present the method will return the first
	 * method with name <code>methodNameToMock</code>. If no method was found,
	 * <code>null</code> will be returned. If no <code>methodName</code> is
	 * specified the method will be found based on the parameter types. If
	 * neither method name nor parameters are specified an
	 * {@link IllegalArgumentException} will be thrown.
	 * 
	 * @param <T>
	 * @param type
	 * @param methodName
	 * @param parameterTypes
	 * @return
	 */
	public static <T> Method findMethod(Class<T> type, String methodName, Class<?>... parameterTypes) {
		if (methodName == null && parameterTypes == null) {
			throw new IllegalArgumentException("You must specify a method name or parameter types.");
		}
		List<Method> matchingMethodsList = new LinkedList<Method>();
		for (Method method : getAllMethods(type)) {
			if (methodName == null || method.getName().equals(methodName)) {
				if (parameterTypes != null && parameterTypes.length > 0) {
					// If argument types was supplied, make sure that they
					// match.
					Class<?>[] paramTypes = method.getParameterTypes();
					if (!checkIfTypesAreSame(parameterTypes, paramTypes)) {
						continue;
					}
				}
				// Add the method to the matching methods list.
				matchingMethodsList.add(method);
			}
		}

		Method methodToMock = null;
		if (matchingMethodsList.size() > 0) {
			if (matchingMethodsList.size() == 1) {
				// We've found a unique method match.
				methodToMock = matchingMethodsList.get(0);
			} else if (parameterTypes.length == 0) {
				/*
				 * If we've found several matches and we've supplied no
				 * parameter types, go through the list of found methods and see
				 * if we have a method with no parameters. In that case return
				 * that method.
				 */
				for (Method method : matchingMethodsList) {
					if (method.getParameterTypes().length == 0) {
						methodToMock = method;
						break;
					}
				}

				if (methodToMock == null) {
					WhiteboxImpl.throwExceptionWhenMultipleMethodMatchesFound("argument parameter types", matchingMethodsList.toArray(new Method[0]));
				}
			} else {
				// We've found several matching methods.
				WhiteboxImpl.throwExceptionWhenMultipleMethodMatchesFound("argument parameter types", matchingMethodsList.toArray(new Method[0]));
			}
		}

		return methodToMock;
	}

	public static boolean isProxy(Class<?> type) {
		return proxyFramework.isProxy(type);
	}

	public static <T> Class<?> getUnmockedType(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}

		Class<?> unmockedType = null;
		if (proxyFramework != null && proxyFramework.isProxy(type)) {
			unmockedType = proxyFramework.getUnproxiedType(type);
		} else if (Proxy.isProxyClass(type)) {
			unmockedType = type.getInterfaces()[0];
		} else {
			unmockedType = type;
		}
		return unmockedType;
	}

	static void throwExceptionWhenMultipleMethodMatchesFound(String helpInfo, Method[] methods) {
		if (methods == null || methods.length < 2) {
			throw new IllegalArgumentException("Internal error: throwExceptionWhenMultipleMethodMatchesFound needs at least two methods.");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Several matching methods found, please specify the ");
		sb.append(helpInfo);
		sb.append(" so that PowerMock can determine which method you're refering to.\n");
		sb.append("Matching methods in class ").append(methods[0].getDeclaringClass().getName()).append(" were:\n");

		for (Method method : methods) {
			sb.append(method.getReturnType().getName()).append(" ");
			sb.append(method.getName()).append("( ");
			final Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> paramType : parameterTypes) {
				sb.append(paramType.getName()).append(".class ");
			}
			sb.append(")\n");
		}
		throw new TooManyMethodsFoundException(sb.toString());
	}

	static void throwExceptionWhenMultipleConstructorMatchesFound(Constructor<?>[] constructors) {
		if (constructors == null || constructors.length < 2) {
			throw new IllegalArgumentException("Internal error: throwExceptionWhenMultipleConstructorMatchesFound needs at least two constructors.");
		}
		StringBuilder sb = new StringBuilder();
		sb
				.append("Several matching constructors found, please specify the argument parameter types so that PowerMock can determine which method you're refering to.\n");
		sb.append("Matching constructors in class ").append(constructors[0].getDeclaringClass().getName()).append(" were:\n");

		for (Constructor<?> constructor : constructors) {
			sb.append(constructor.getName()).append("( ");
			final Class<?>[] parameterTypes = constructor.getParameterTypes();
			for (Class<?> paramType : parameterTypes) {
				sb.append(paramType.getName()).append(".class ");
			}
			sb.append(")\n");
		}
		throw new TooManyConstructorsFoundException(sb.toString());
	}

	@SuppressWarnings("all")
	public static Method findMethodOrThrowException(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Method methodToMock = findMethod(type, methodName, parameterTypes);
		throwExceptionIfMethodWasNotFound(type, methodName, methodToMock, parameterTypes);
		return methodToMock;
	}

	/**
	 * Get an array of {@link Method}'s that matches the supplied list of method
	 * names. Both instance and static methods are taken into account.
	 * 
	 * @param clazz
	 *            The class that should contain the methods.
	 * @param methodNames
	 *            Names of the methods that will be returned.
	 * @return An array of Method's. May be of length 0 but not
	 *         <code>null</code>.
	 * @throws MethodNotFoundException
	 *             If no method was found.
	 */
	public static Method[] getMethods(Class<?> clazz, String... methodNames) {
		if (methodNames == null || methodNames.length == 0) {
			throw new IllegalArgumentException("You must supply at least one method name.");
		}
		final List<Method> methodsToMock = new LinkedList<Method>();
		Method[] allMethods = null;
		if (clazz.isInterface()) {
			allMethods = getAllPublicMethods(clazz);
		} else {
			allMethods = getAllMethods(clazz);
		}

		for (Method method : allMethods) {
			for (String methodName : methodNames) {
				if (method.getName().equals(methodName)) {
					method.setAccessible(true);
					methodsToMock.add(method);
				}
			}
		}

		final Method[] methodArray = methodsToMock.toArray(new Method[0]);
		if (methodArray.length == 0) {
			throw new MethodNotFoundException(String.format("No methods matching the name(s) %s were found in the class hierarchy of %s.",
					concatenateStrings(methodNames), getType(clazz)));
		}
		return methodArray;
	}

	/**
	 * Get an array of {@link Field}'s that matches the supplied list of field
	 * names. Both instance and static fields are taken into account.
	 * 
	 * @param clazz
	 *            The class that should contain the fields.
	 * @param fieldNames
	 *            Names of the fields that will be returned.
	 * @return An array of Field's. May be of length 0 but not <code>null</code>
	 *         .
	 */
	public static Field[] getFields(Class<?> clazz, String... fieldNames) {
		final List<Field> fields = new LinkedList<Field>();

		for (Field field : getAllFields(clazz)) {
			for (String fieldName : fieldNames) {
				if (field.getName().equals(fieldName)) {
					fields.add(field);
				}
			}
		}

		final Field[] fieldArray = fields.toArray(new Field[fields.size()]);
		if (fieldArray.length == 0) {
			throw new FieldNotFoundException(String.format("No fields matching the name(s) %s were found in the class hierarchy of %s.",
					concatenateStrings(fieldNames), getType(clazz)));
		}
		return fieldArray;
	}

	@SuppressWarnings("unchecked")
	public static <T> T performMethodInvocation(Object tested, Method methodToInvoke, Object... arguments) throws Exception {
		final boolean accessible = methodToInvoke.isAccessible();
		if (!accessible) {
			methodToInvoke.setAccessible(true);
		}
		try {
			if (isPotentialVarArgsMethod(methodToInvoke, arguments)) {
				Class<?>[] parameterTypes = methodToInvoke.getParameterTypes();
				final int varArgsIndex = parameterTypes.length - 1;
				Class<?> varArgsType = parameterTypes[varArgsIndex].getComponentType();
				Object varArgsArrayInstance = createAndPopulateVarArgsArray(varArgsType, varArgsIndex, arguments);
				Object[] completeArgumentList = new Object[parameterTypes.length];
				for (int i = 0; i < varArgsIndex; i++) {
					completeArgumentList[i] = arguments[i];
				}
				completeArgumentList[completeArgumentList.length - 1] = varArgsArrayInstance;
				return (T) methodToInvoke.invoke(tested, completeArgumentList);
			} else {
				return (T) methodToInvoke.invoke(tested, arguments == null ? new Object[] { arguments } : arguments);
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				throw new MethodInvocationException(cause);
			}
		} finally {
			if (!accessible) {
				methodToInvoke.setAccessible(false);
			}
		}
	}

	public static <T> Method[] getAllMethodExcept(Class<T> type, String... methodNames) {
		List<Method> methodsToMock = new LinkedList<Method>();
		Method[] methods = getAllMethods(type);
		iterateMethods: for (Method method : methods) {
			for (String methodName : methodNames) {
				if (method.getName().equals(methodName)) {
					continue iterateMethods;
				}
			}
			methodsToMock.add(method);
		}
		return methodsToMock.toArray(new Method[0]);
	}

	public static <T> Method[] getAllMetodsExcept(Class<T> type, String methodNameToExclude, Class<?>[] argumentTypes) {
		Method[] methods = getAllMethods(type);
		List<Method> methodList = new ArrayList<Method>();
		outer: for (Method method : methods) {
			if (method.getName().equals(methodNameToExclude)) {
				if (argumentTypes != null && argumentTypes.length > 0) {
					final Class<?>[] args = method.getParameterTypes();
					if (args != null && args.length == argumentTypes.length) {
						for (int i = 0; i < args.length; i++) {
							if (args[i].isAssignableFrom(getUnmockedType(argumentTypes[i]))) {
								/*
								 * Method was not found thus it should not be
								 * mocked. Continue to investigate the next
								 * method.
								 */
								continue outer;
							}
						}
					}
				} else {
					continue;
				}
			}
			methodList.add(method);
		}
		return methodList.toArray(new Method[0]);
	}

	public static boolean areAllMethodsStatic(Method... methods) {
		for (Method method : methods) {
			if (!Modifier.isStatic(method.getModifiers())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if all arguments are of the same type.
	 */
	static boolean areAllArgumentsOfSameType(Object[] arguments) {
		if (arguments == null || arguments.length <= 1) {
			return true;
		}

		// Handle null values
		int index = 0;
		Object object = null;
		while (object == null && index < arguments.length) {
			object = arguments[index++];
		}

		if (object == null) {
			return true;
		}
		// End of handling null values

		final Class<?> firstArgumentType = getType(object);
		for (int i = index; i < arguments.length; i++) {
			final Object argument = arguments[i];
			if (argument != null && !getType(argument).isAssignableFrom(firstArgumentType)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @return <code>true</code> if all actual parameter types are assignable
	 *         from the expected arguments, <code>false</code> otherwise.
	 */
	private static boolean checkIfTypesAreSame(Class<?>[] parameterTypes, Object[] arguments) {
		if (parameterTypes == null) {
			throw new IllegalArgumentException("parameter types cannot be null");
		} else if (parameterTypes.length != arguments.length) {
			return false;
		}
		for (int i = 0; i < parameterTypes.length; i++) {
			Object argument = arguments[i];
			if (argument == null) {
				continue;
			} else {
				if (!parameterTypes[i].isAssignableFrom(getType(argument)) && !(parameterTypes[i].equals(Class.class) && isClass(argument))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @return The type of the of an object.
	 */
	public static Class<?> getType(Object object) {
		Class<?> type = null;
		if (isClass(object)) {
			type = (Class<?>) object;
		} else if (object != null) {
			type = object.getClass();
		}
		return getUnmockedType(type);
	}

	/**
	 * Get an inner class type
	 * 
	 * @param declaringClass
	 *            The class in which the inner class is declared.
	 * @param name
	 *            The unqualified name (simple name) of the inner class.
	 * @return The type.
	 */
	@SuppressWarnings("unchecked")
	public static Class<Object> getInnerClassType(Class<?> declaringClass, String name) throws ClassNotFoundException {
		return (Class<Object>) Class.forName(declaringClass.getName() + "$" + name);
	}

	/**
	 * Get the type of a local inner class.
	 * 
	 * @param declaringClass
	 *            The class in which the local inner class is declared.
	 * @param occurrence
	 *            The occurrence of the local class. For example if you have two
	 *            local classes in the <code>declaringClass</code> you must pass
	 *            in <code>1</code> if you want to get the type for the first
	 *            one or <code>2</code> if you want the second one.
	 * @param name
	 *            The unqualified name (simple name) of the local class.
	 * @return The type.
	 */
	@SuppressWarnings("unchecked")
	public static Class<Object> getLocalClassType(Class<?> declaringClass, int occurrence, String name) throws ClassNotFoundException {
		return (Class<Object>) Class.forName(declaringClass.getName() + "$" + occurrence + name);
	}

	/**
	 * Get the type of an anonymous inner class.
	 * 
	 * @param declaringClass
	 *            The class in which the anonymous inner class is declared.
	 * @param occurrence
	 *            The occurrence of the anonymous inner class. For example if
	 *            you have two anonymous inner classes classes in the
	 *            <code>declaringClass</code> you must pass in <code>1</code> if
	 *            you want to get the type for the first one or <code>2</code>
	 *            if you want the second one.
	 * @return The type.
	 */
	@SuppressWarnings("unchecked")
	public static Class<Object> getAnonymousInnerClassType(Class<?> declaringClass, int occurrence) throws ClassNotFoundException {
		return (Class<Object>) Class.forName(declaringClass.getName() + "$" + occurrence);
	}

	/**
	 * Get all fields annotated with a particular annotation. This method
	 * traverses the class hierarchy when checking for the annotation.
	 * 
	 * @param object
	 *            The object to look for annotations. Note that if're you're
	 *            passing an object only instance fields are checked, passing a
	 *            class will only check static fields.
	 * @param annotation
	 *            The annotation type to look for.
	 * @param additionalAnnotations
	 *            Optionally more annotations to look for. If any of the
	 *            annotations are associated with a particular field it will be
	 *            added to the resulting <code>Set</code>.
	 * @return A set of all fields containing the particular annotation.
	 */
	@SuppressWarnings("unchecked")
	public static Set<Field> getFieldsAnnotatedWith(Object object, Class<? extends Annotation> annotation,
			Class<? extends Annotation>... additionalAnnotations) {
		Class<? extends Annotation>[] annotations = null;
		if (additionalAnnotations == null || additionalAnnotations.length == 0) {
			annotations = (Class<? extends Annotation>[]) new Class<?>[] { annotation };
		} else {
			annotations = (Class<? extends Annotation>[]) new Class<?>[additionalAnnotations.length + 1];
			annotations[0] = annotation;
			System.arraycopy(additionalAnnotations, 0, annotations, 1, additionalAnnotations.length);
		}
		return getFieldsAnnotatedWith(object, annotations);
	}

	/**
	 * Get all fields annotated with a particular annotation. This method
	 * traverses the class hierarchy when checking for the annotation.
	 * 
	 * @param object
	 *            The object to look for annotations. Note that if're you're
	 *            passing an object only instance fields are checked, passing a
	 *            class will only check static fields.
	 * @param annotationTypes
	 *            The annotation types to look for
	 * @return A set of all fields containing the particular annotation(s).
	 * @since 1.3
	 */
	public static Set<Field> getFieldsAnnotatedWith(Object object, Class<? extends Annotation>[] annotationTypes) {
		return findAllFieldsUsingStrategy(new FieldAnnotationMatcherStrategy(annotationTypes), object, true, getType(object));
	}

	/**
	 * Get all fields assignable from a particular type. This method traverses
	 * the class hierarchy when checking for the type.
	 * 
	 * @param object
	 *            The object to look for type. Note that if're you're passing an
	 *            object only instance fields are checked, passing a class will
	 *            only check static fields.
	 * @param type
	 *            The type to look for.
	 * @return A set of all fields of the particular type.
	 */
	public static Set<Field> getFieldsOfType(Object object, Class<?> type) {
		return findAllFieldsUsingStrategy(new AssignableFromFieldTypeMatcherStrategy(type), object, true, getType(object));
	}

	/**
	 * Get all instance fields for a particular object. It returns all fields
	 * regardless of the field modifier and regardless of where in the class
	 * hierarchy a field is located.
	 * 
	 * @param object
	 *            The object whose instance fields to get.
	 * @return All instance fields in the hierarchy. All fields are set to
	 *         accessible
	 */
	public static Set<Field> getAllInstanceFields(Object object) {
		return findAllFieldsUsingStrategy(new AllFieldsMatcherStrategy(), object, true, getType(object));
	}

	/**
	 * Get all static fields for a particular type.
	 * 
	 * @param type
	 *            The class whose static fields to get.
	 * @return All static fields in <code>type</code>. All fields are set to
	 *         accessible.
	 */
	public static Set<Field> getAllStaticFields(Class<?> type) {
		final Set<Field> fields = new LinkedHashSet<Field>();
		final Field[] declaredFields = type.getDeclaredFields();
		for (Field field : declaredFields) {
			if (Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				fields.add(field);
			}
		}
		return fields;
	}

	public static boolean isClass(Object argument) {
		return argument instanceof Class<?>;
	}

	/**
	 * @return <code>true</code> if all actual parameter types are assignable
	 *         from the expected parameter types, <code>false</code> otherwise.
	 */
	private static boolean checkIfTypesAreSame(Class<?>[] expectedParameterTypes, Class<?>[] actualParameterTypes) {
		if (expectedParameterTypes == null || actualParameterTypes == null) {
			throw new IllegalArgumentException("parameter types cannot be null");
		} else if (expectedParameterTypes.length != actualParameterTypes.length) {
			return false;
		} else {
			for (int i = 0; i < expectedParameterTypes.length; i++) {
				if (!expectedParameterTypes[i].isAssignableFrom(getType(actualParameterTypes[i]))) {
					return false;
				}
			}
		}
		return true;
	}

	private static Field getField(String fieldName, Class<?> where) {
		if (where == null) {
			throw new IllegalArgumentException("where cannot be null");
		}

		Field field = null;
		try {
			field = where.getDeclaredField(fieldName);
			field.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new FieldNotFoundException("Field '" + fieldName + "' was not found in class " + where.getName() + ".");
		}
		return field;
	}

	private static Field findFieldOrThrowException(Class<?> fieldType, Class<?> where) {
		if (fieldType == null || where == null) {
			throw new IllegalArgumentException("fieldType and where cannot be null");
		}
		Field field = null;
		for (Field currentField : where.getDeclaredFields()) {
			currentField.setAccessible(true);
			if (currentField.getType().equals(fieldType)) {
				field = currentField;
				break;
			}
		}
		if (field == null) {
			throw new FieldNotFoundException("Cannot find a field of type " + fieldType + "in where.");
		}
		return field;
	}

	private static void setField(Object object, Object value, Field foundField) {
		foundField.setAccessible(true);
		try {
			foundField.set(object, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Internal error: Failed to set field in method setInternalState.", e);
		}
	}

	private static String concatenateStrings(String... stringsToConcatenate) {
		StringBuilder builder = new StringBuilder();
		final int stringsLength = stringsToConcatenate.length;
		for (int i = 0; i < stringsLength; i++) {
			if (i == stringsLength - 1 && stringsLength != 1) {
				builder.append(" or ");
			} else if (i != 0) {
				builder.append(", ");
			}
			builder.append(stringsToConcatenate[i]);
		}
		return builder.toString();
	}

	private static boolean isPotentialVarArgsMethod(Method method, Object[] arguments) {
		return doesParameterTypesMatchForVarArgsInvocation(method.isVarArgs(), method.getParameterTypes(), arguments);
	}

	private static boolean isPotentialVarArgsConstructor(Constructor<?> constructor, Object[] arguments) {
		final Class<?>[] parameterTypes = constructor.getParameterTypes();
		return doesParameterTypesMatchForVarArgsInvocation(constructor.isVarArgs(), parameterTypes, arguments);
	}

	private static boolean doesParameterTypesMatchForVarArgsInvocation(boolean isVarArgs, Class<?>[] parameterTypes, Object[] arguments) {
		if (isVarArgs && arguments != null && arguments.length >= 1 && parameterTypes != null && parameterTypes.length >= 1) {
			final Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
			final Object lastArgument = arguments[arguments.length - 1];
			if (lastArgument != null) {
				final Class<?> firstArgumentTypeAsPrimitive = getTypeAsPrimitiveIfWrapped(lastArgument);
				final Class<?> varArgsParameterTypeAsPrimitive = getTypeAsPrimitiveIfWrapped(componentType);
				isVarArgs = isVarArgs && varArgsParameterTypeAsPrimitive.isAssignableFrom(firstArgumentTypeAsPrimitive);
			}
		}
		return isVarArgs && areAllArgumentsOfSameType(arguments);
	}

	/**
	 * Get the type of an object and convert it to primitive if the type has a
	 * primitive counter-part. E.g. if object is an instance of
	 * <code>java.lang.Integer</code> this method will return
	 * <code>int.class</code>.
	 * 
	 * @param object
	 *            The object whose type to get.
	 */
	private static Class<?> getTypeAsPrimitiveIfWrapped(Object object) {
		if (object != null) {
			final Class<?> firstArgumentType = getType(object);
			final Class<?> firstArgumentTypeAsPrimitive = PrimitiveWrapper.hasPrimitiveCounterPart(firstArgumentType) ? PrimitiveWrapper
					.getPrimitiveFromWrapperType(firstArgumentType) : firstArgumentType;
			return firstArgumentTypeAsPrimitive;
		}
		return null;
	}

	public static void setInternalStateFromContext(Object object, Object context, Object[] additionalContexts) {
		if (!isClass(context)) {
			copyState(object, context);
		}
		copyState(object, getType(object));
		if (additionalContexts != null && additionalContexts.length > 0) {
			for (Object additionContext : additionalContexts) {
				if (!isClass(additionContext)) {
					copyState(object, additionContext);
				}
				copyState(object, getType(additionContext));
			}
		}
	}

	public static void setInternalStateFromContext(Object object, Class<?> context, Class<?>[] additionalContexts) {
		copyState(object, context);
		if (additionalContexts != null && additionalContexts.length > 0) {
			for (Class<?> additionContext : additionalContexts) {
				copyState(object, additionContext);
			}
		}
	}

	static void copyState(Object object, Object context) {
		if (object == null) {
			throw new IllegalArgumentException("object to set state cannot be null");
		} else if (context == null) {
			throw new IllegalArgumentException("context cannot be null");
		}

		Set<Field> allFields = isClass(context) ? getAllStaticFields(getType(context)) : getAllInstanceFields(context);
		for (Field field : allFields) {
			try {
				final boolean isStaticField = Modifier.isStatic(field.getModifiers());
				setInternalState(isStaticField ? getType(object) : object, field.getType(), field.get(context));
			} catch (IllegalAccessException e) {
				// Should never happen
				throw new RuntimeException("Internal Error: Failed to get the field value in method setInternalStateFromContext.", e);
			}
		}
	}
}
