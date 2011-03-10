package com.testpowermock;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestClassWithStaticFinalMethod implements IPrepareForTest {

	@Override
	public Class<?>[] classesToPrepare() {
		return new Class<?>[] { ClassWithStaticFinalMethod.class };
	}

	@Test
	public void testGetString() {
		PowerMockito.mockStatic(ClassWithStaticFinalMethod.class);
		PowerMockito.when(ClassWithStaticFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticFinalMethod.getString());

	}

}
