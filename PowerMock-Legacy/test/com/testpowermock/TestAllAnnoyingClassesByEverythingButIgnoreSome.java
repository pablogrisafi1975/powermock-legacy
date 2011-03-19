package com.testpowermock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPowerMockIgnore;
import org.powermock.core.classloader.interfaces.IPrepareEverythingForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestAllAnnoyingClassesByEverythingButIgnoreSome implements IPrepareEverythingForTest, IPowerMockIgnore {
	
	@Override
	public String[] fullyQualifiedNamesToIgnore() {
		return new String[]{ "com.testpowermock.ClassWithStaticFinalMethod" };
	}

	@Test
	public void testClassWithFinalMethod_GetString() {
		ClassWithFinalMethod classWithFinalMethod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}

	@Test
	public void testClassWithStaticFinalMethod_GetString() {
		Exception ex = null;
		try {
			PowerMockito.mockStatic(ClassWithStaticFinalMethod.class);
		} catch (ClassCastException cce) {
			ex = cce;
		}
		assertNotNull(ex);
		assertEquals("ORIGINAL_STRING", ClassWithStaticFinalMethod.getString());
	}

	@Test
	public void testClassWithStaticMethod_GetString() {
		PowerMockito.mockStatic(ClassWithStaticMethod.class);
		PowerMockito.when(ClassWithStaticMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticMethod.getString());
	}

	@Test
	public void testFinalClassWithFinalMethod_GetString() {
		FinalClassWithFinalMethod finalClassWithFinalMethod = PowerMockito.mock(FinalClassWithFinalMethod.class);
		PowerMockito.when(finalClassWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", finalClassWithFinalMethod.getString());
	}



}
