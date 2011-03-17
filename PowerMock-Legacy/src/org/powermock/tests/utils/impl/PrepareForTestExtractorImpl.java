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
import org.powermock.core.classloader.interfaces.IPrepareForTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.core.classloader.interfaces.IPrepareOnlyThisForTest;
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
	protected String[] getClassesToModify(AnnotatedElement element) {
		Set<String> all = new LinkedHashSet<String>();
		PrepareInfo prepareInfo = new PrepareInfo(element);

		if (!prepareInfo.preparePresent && !prepareInfo.prepareOnlyThisPresent) {
			return null;
		}

		if (prepareInfo.preparePresent) {
			for (Class<?> classToMock : prepareInfo.classesToMock) {
				if (!classToMock.equals(IndicateReloadClass.class)) {
					addClassHierarchy(all, classToMock);
				}
			}
			addFullyQualifiedNames(all, prepareInfo.fullyQualifiedNamesToMock);
		}
		if (prepareInfo.prepareOnlyThisPresent) {
			for (Class<?> classToMock : prepareInfo.classesToMockOnlyThis) {
				if (!classToMock.equals(IndicateReloadClass.class)) {
					all.add(classToMock.getName());
				}
			}
			addFullyQualifiedNames(all, prepareInfo.fullyQualifiedNamesToMockOnlyThis);
		}

		return all.toArray(new String[0]);

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

	/**
	 * The information you need to know if and how this class / method should be
	 * mocked
	 * 
	 * @author Pablo
	 * 
	 */
	static class PrepareInfo {
		public PrepareInfo(AnnotatedElement element) {
			PrepareForTest prepareForTestAnnotation = element.getAnnotation(PrepareForTest.class);
			PrepareOnlyThisForTest prepareOnlyThisForTestAnnotation = element
					.getAnnotation(PrepareOnlyThisForTest.class);
			boolean prepareForTestAnnotationPresent = prepareForTestAnnotation != null;
			boolean prepareForTestInterfacePresent = false;
			boolean prepareOnlyThisForTestAnnotationPresent = prepareOnlyThisForTestAnnotation != null;
			boolean prepareOnlyThisForTestInterfacePresent = false;

			if (prepareForTestAnnotationPresent || prepareOnlyThisForTestAnnotationPresent) {
				// Annotation based, the java 1.5 version
				if (prepareForTestAnnotationPresent) {
					classesToMock = prepareForTestAnnotation.value();
					fullyQualifiedNamesToMock = prepareForTestAnnotation.fullyQualifiedNames();
				}
				if (prepareOnlyThisForTestAnnotationPresent) {
					classesToMockOnlyThis = prepareOnlyThisForTestAnnotation.value();
					fullyQualifiedNamesToMockOnlyThis = prepareOnlyThisForTestAnnotation.fullyQualifiedNames();
				}
			} else {
				// Interface base, the java 1.4 version
				if (element instanceof Class) {
					// No way to do that for methods.....
					Class<?> clazzTest = (Class<?>) element;
					prepareForTestInterfacePresent = IPrepareForTest.class.isAssignableFrom(clazzTest);
					prepareOnlyThisForTestInterfacePresent = IPrepareOnlyThisForTest.class.isAssignableFrom(clazzTest);
					if (!prepareForTestInterfacePresent && !prepareOnlyThisForTestInterfacePresent) {
						this.preparePresent = false;
						this.prepareOnlyThisPresent = false;
						return;
					}
					Object testInstance = null;
					try {
						testInstance = clazzTest.newInstance();

					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException("Can't instantiate class " + clazzTest, e);
					}
					if (prepareForTestInterfacePresent) {
						IPrepareForTest prepareForTestByClass = (IPrepareForTest) testInstance;
						this.classesToMock = coalesce(prepareForTestByClass.classesToPrepare(), DEFAULT_CLASSES);
						this.fullyQualifiedNamesToMock = coalesce(prepareForTestByClass.fullyQualifiedNamesToPrepare(),
								DEFAULT_FULLY_QUALIFIED_NAMES);
					}
					if (prepareOnlyThisForTestInterfacePresent) {
						IPrepareOnlyThisForTest prepareOnlyThisForTestByClass = (IPrepareOnlyThisForTest) testInstance;
						this.classesToMockOnlyThis = coalesce(prepareOnlyThisForTestByClass.classesToPrepareOnlyThis(),
								DEFAULT_CLASSES);
						this.fullyQualifiedNamesToMockOnlyThis = coalesce(
								prepareOnlyThisForTestByClass.fullyQualifiedNamesToPrepareOnlyThis(),
								DEFAULT_FULLY_QUALIFIED_NAMES);
					}
				}
			}
			this.prepareOnlyThisPresent = prepareOnlyThisForTestAnnotationPresent
					|| prepareOnlyThisForTestInterfacePresent;
			this.preparePresent = prepareForTestAnnotationPresent || prepareForTestInterfacePresent;
		}

		private static final Class<?>[] DEFAULT_CLASSES = new Class<?>[] { IndicateReloadClass.class };
		private static final String[] DEFAULT_FULLY_QUALIFIED_NAMES = new String[] { "" };

		private static <T> T coalesce(T val1, T val2) {
			if (val1 != null)
				return val1;
			return val2;
		}

		private final boolean preparePresent;
		private Class<?>[] classesToMock;
		private String[] fullyQualifiedNamesToMock;
		private final boolean prepareOnlyThisPresent;
		private Class<?>[] classesToMockOnlyThis;
		private String[] fullyQualifiedNamesToMockOnlyThis;

	}
}
