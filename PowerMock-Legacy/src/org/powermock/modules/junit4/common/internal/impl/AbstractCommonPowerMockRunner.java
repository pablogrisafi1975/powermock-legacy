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
package org.powermock.modules.junit4.common.internal.impl;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.powermock.modules.junit4.common.internal.JUnit4TestSuiteChunker;
import org.powermock.modules.junit4.common.internal.PowerMockJUnitRunnerDelegate;

public abstract class AbstractCommonPowerMockRunner extends Runner implements Filterable, Sortable {

	private JUnit4TestSuiteChunker suiteChunker;

	public AbstractCommonPowerMockRunner(Class<?> klass, Class<? extends PowerMockJUnitRunnerDelegate> runnerDelegateImplClass) throws Exception {
		suiteChunker = new JUnit4TestSuiteChunkerImpl(klass, runnerDelegateImplClass);
	}

	@Override
	public Description getDescription() {
		return suiteChunker.getDescription();
	}

	@Override
	public void run(RunNotifier notifier) {
		suiteChunker.run(notifier);
	}

	@Override
	public synchronized int testCount() {
		return suiteChunker.getTestCount();
	}

	public void filter(Filter filter) throws NoTestsRemainException {
		suiteChunker.filter(filter);
	}

	public void sort(Sorter sorter) {
		suiteChunker.sort(sorter);
	}
}
