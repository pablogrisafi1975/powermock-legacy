package com.testpowermock;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestClassWithFinalMethod implements IPrepareForTest {

	@Override
	public Class<?>[] classesToPrepare() {
		return new Class<?>[] { ClassWithFinalMethod.class };
	}

	@Test
	public void testGetString() {
		ClassWithFinalMethod classWithFinalMethod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}

}
