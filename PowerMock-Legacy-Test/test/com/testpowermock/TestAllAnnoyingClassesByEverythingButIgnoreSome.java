package com.testpowermock;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPowerMockIgnore;
import org.powermock.core.classloader.interfaces.IPrepareEverythingForTest;
import org.powermock.modules.junit3.PowerMockSuite;

public class TestAllAnnoyingClassesByEverythingButIgnoreSome extends TestCase implements IPrepareEverythingForTest,
		IPowerMockIgnore {

	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}

	public String[] fullyQualifiedNamesToIgnore() {
		return new String[] { "com.testpowermock.ClassWithStaticFinalMethod" };
	}

	public void testClassWithFinalMethod_GetString() {
		ClassWithFinalMethod classWithFinalMethod = (ClassWithFinalMethod) PowerMockito
				.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}

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

	public void testClassWithStaticMethod_GetString() {
		PowerMockito.mockStatic(ClassWithStaticMethod.class);
		PowerMockito.when(ClassWithStaticMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticMethod.getString());
	}

	public void testFinalClassWithFinalMethod_GetString() {
		FinalClassWithFinalMethod finalClassWithFinalMethod = (FinalClassWithFinalMethod) PowerMockito
				.mock(FinalClassWithFinalMethod.class);
		PowerMockito.when(finalClassWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", finalClassWithFinalMethod.getString());
	}

}
