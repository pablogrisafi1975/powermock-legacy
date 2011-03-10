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
package org.powermock.api.mockito.internal;

import org.mockito.Mockito;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.expectation.PowerMockitoStubber;
import org.powermock.api.mockito.internal.expectation.PowerMockitoStubberImpl;
import org.powermock.reflect.Whitebox;

public class PowerMockitoCore {
	@SuppressWarnings("unchecked")
	public PowerMockitoStubber doAnswer(Answer answer) {
		getMockingProgress().stubbingStarted();
		getMockingProgress().resetOngoingStubbing();
		return (PowerMockitoStubber) new PowerMockitoStubberImpl().doAnswer(answer);
	}

	private MockingProgress getMockingProgress() {
		return Whitebox.getInternalState(Mockito.class, MockingProgress.class);
	}

}
