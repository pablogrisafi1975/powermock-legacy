package com.testpowermock;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ClassWithFinalMethod.class)
public class TestClassWithFinalMethodClassic {

	@Test
	public void getString() {
		ClassWithFinalMethod classWithFinalMethod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}
}
