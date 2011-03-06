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
package org.powermock.tests.utils.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.powermock.core.IndicateReloadClass;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.core.classloader.annotations.IPrepareOnlyThisForTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.tests.utils.TestClassesExtractor;

/**
 * Implementation of the {@link TestClassesExtractor} interface that extract
 * classes from the {@link PrepareForTest} or {@link PrepareOnlyThisForTest}
 * annotations.
 * 
 */
public class PrepareForTestExtractorImpl extends AbstractTestClassExtractor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String[] getClassesToModify(AnnotatedElement element) {
		Set<String> all = new LinkedHashSet<String>();

		PrepareForTest prepareForTestAnnotation = element.getAnnotation(PrepareForTest.class);
		PrepareOnlyThisForTest prepareOnlyThisForTestAnnotation = element.getAnnotation(PrepareOnlyThisForTest.class);
		final boolean prepareForTestAnnotationPresent = prepareForTestAnnotation != null;
		final boolean prepareOnlyThisForTestAnnotationPresent = prepareOnlyThisForTestAnnotation != null;
		boolean prepareForTestInterfacePresent = false;
		if (element instanceof Class) {
			Class<?> clazz = (Class<?>) element;
			prepareForTestInterfacePresent = IPrepareForTest.class.isAssignableFrom(clazz);
		}

		boolean prepareOnlyThisForTestInterfacePresent = false;
		if (element instanceof Class) {
			Class<?> clazz = (Class<?>) element;
			prepareOnlyThisForTestInterfacePresent = IPrepareOnlyThisForTest.class.isAssignableFrom(clazz);
		}

		if (!prepareForTestAnnotationPresent && !prepareOnlyThisForTestAnnotationPresent && !prepareForTestInterfacePresent && !prepareOnlyThisForTestInterfacePresent) {
			return null;
		}

			if (prepareForTestAnnotationPresent || prepareForTestInterfacePresent) {
			Class<?>[] classesToMock = null;
			if (prepareForTestAnnotationPresent) {
				classesToMock = prepareForTestAnnotation.value();
			} else {
				classesToMock = getValues(element);
			}
			for (Class<?> classToMock : classesToMock) {
				if (!classToMock.equals(IndicateReloadClass.class)) {
					addClassHierarchy(all, classToMock);
				}
			}
			if (prepareForTestAnnotationPresent) {
				addFullyQualifiedNames(all, prepareForTestAnnotation);
			}
		}

			if (prepareOnlyThisForTestAnnotationPresent || prepareOnlyThisForTestInterfacePresent) {
			Class<?>[] classesToMock = null;
			if (prepareOnlyThisForTestAnnotationPresent) {
				classesToMock = prepareOnlyThisForTestAnnotation.value();
			} else {
				classesToMock = getValues(element);
			}
			for (Class<?> classToMock : classesToMock) {
				if (!classToMock.equals(IndicateReloadClass.class)) {
					all.add(classToMock.getName());
				}
			}
			if (prepareOnlyThisForTestAnnotationPresent) {
				addFullyQualifiedNames(all, prepareOnlyThisForTestAnnotation);
				}
		}

	        return all.toArray(new String[0]);

	}

	private Class<?>[] getValues(AnnotatedElement element) {
		Class<?> clazz = (Class<?>) element;
		try {
			Object newInstance = clazz.newInstance();
			if (newInstance instanceof IPrepareForTest) {
				return ((IPrepareForTest) newInstance).classesToPrepare();
			} else {
				return ((IPrepareOnlyThisForTest) newInstance).values();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return new Class<?>[] {};
		}
	}

	private void addFullyQualifiedNames(Set<String> all, PrepareForTest annotation) {
		String[] fullyQualifiedNames = annotation.fullyQualifiedNames();
		addFullyQualifiedNames(all, fullyQualifiedNames);
	}

	private void addFullyQualifiedNames(Set<String> all, PrepareOnlyThisForTest annotation) {
		String[] fullyQualifiedNames = annotation.fullyQualifiedNames();
		addFullyQualifiedNames(all, fullyQualifiedNames);
	}

	private void addFullyQualifiedNames(Set<String> all, String[] fullyQualifiedNames) {
		for (String string : fullyQualifiedNames) {
			if (!"".equals(string)) {
				all.add(string);
			}
		}
	}

	private void addClassHierarchy(Set<String> all, Class<?> classToMock) {
		while (classToMock != null && !classToMock.equals(Object.class)) {
			addInnerClassesAndInterfaces(all, classToMock);
			all.add(classToMock.getName());
			classToMock = classToMock.getSuperclass();
		}
	}

	private void addInnerClassesAndInterfaces(Set<String> all, Class<?> classToMock) {
		Class<?>[] declaredClasses = classToMock.getDeclaredClasses();
		for (Class<?> innerClass : declaredClasses) {
			all.add(innerClass.getName());
		}
	}
}
