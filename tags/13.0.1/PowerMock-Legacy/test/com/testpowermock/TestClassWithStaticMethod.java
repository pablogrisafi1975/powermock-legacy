package com.testpowermock;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestClassWithStaticMethod implements IPrepareForTest {

	@Override
	public Class<?>[] classesToPrepare() {
		return new Class<?>[] { ClassWithStaticMethod.class };
	}

	@Test
	public void testGetString() {
		PowerMockito.mockStatic(ClassWithStaticMethod.class);
		PowerMockito.when(ClassWithStaticMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticMethod.getString());
	}



}
