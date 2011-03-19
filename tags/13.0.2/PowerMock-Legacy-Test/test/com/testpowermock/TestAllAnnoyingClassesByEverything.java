package com.testpowermock;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareEverythingForTest;
import org.powermock.modules.junit3.PowerMockSuite;


public class TestAllAnnoyingClassesByEverything extends TestCase implements IPrepareEverythingForTest {

	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}
	
	public void testClassWithFinalMethod_GetString() {
		ClassWithFinalMethod classWithFinalMethod = (ClassWithFinalMethod) PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}

	public void testClassWithStaticFinalMethod_GetString() {
		PowerMockito.mockStatic(ClassWithStaticFinalMethod.class);
		PowerMockito.when(ClassWithStaticFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticFinalMethod.getString());

	}

	public void testClassWithStaticMethod_GetString() {
		PowerMockito.mockStatic(ClassWithStaticMethod.class);
		PowerMockito.when(ClassWithStaticMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticMethod.getString());
	}

	public void testFinalClassWithFinalMethod_GetString() {
		FinalClassWithFinalMethod finalClassWithFinalMethod = (FinalClassWithFinalMethod) PowerMockito.mock(FinalClassWithFinalMethod.class);
		PowerMockito.when(finalClassWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", finalClassWithFinalMethod.getString());

	}

}
