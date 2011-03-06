package com.testpowermock;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.modules.junit3.PowerMockSuite;


public class TestClassWithStaticFinalMethod extends TestCase implements IPrepareForTest {
	
	public Class[] classesToPrepare() {
		return new Class[] { ClassWithStaticFinalMethod.class};
	}
	
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestClassWithStaticFinalMethod.class });
	}
	
	public void testGetString() {
		PowerMockito.mockStatic(ClassWithStaticFinalMethod.class);
		PowerMockito.when(ClassWithStaticFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticFinalMethod.getString());

	}

}
