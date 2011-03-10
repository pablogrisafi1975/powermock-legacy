package com.testpowermock;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.IPrepareForTest;
import org.powermock.modules.junit3.PowerMockSuite;
public class TestClassWithFinalMethod extends TestCase implements IPrepareForTest {
	
	public Class[] classesToPrepare() {
		return new Class[] { ClassWithFinalMethod.class};
	}
	
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}
	
	public void testGetString() {
		ClassWithFinalMethod classWithFinalMethod = (ClassWithFinalMethod) PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}

}
