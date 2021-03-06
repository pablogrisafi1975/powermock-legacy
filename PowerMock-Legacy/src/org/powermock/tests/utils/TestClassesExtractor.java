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
package org.powermock.tests.utils;

import java.lang.reflect.AnnotatedElement;

public interface TestClassesExtractor {
	/**
	 * @return Returns <code>null</code> if the element was not annotated, an
	 *         empty String[] if it is annotated but contains no classes, or a
	 *         string-array of all class names if interest.
	 */
	String[] getTestClasses(AnnotatedElement element);

	boolean isPrepared(AnnotatedElement element, String fullyQualifiedClassName);
}
