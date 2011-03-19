package com.testpowermock;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareForTest;
import org.powermock.modules.junit3.PowerMockSuite;


public class TestFinalClassWithFinalMethod  extends TestCase implements IPrepareForTest {
	
	public Class[] classesToPrepare() {
		return new Class[] { FinalClassWithFinalMethod.class};
	}
	
	public String[] fullyQualifiedNamesToPrepare() {
		return null;
	}
	
	
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestFinalClassWithFinalMethod.class });
	}
	
	public void testGetString() {
		FinalClassWithFinalMethod finalClassWithFinalMethod = (FinalClassWithFinalMethod) PowerMockito.mock(FinalClassWithFinalMethod.class);
		PowerMockito.when(finalClassWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", finalClassWithFinalMethod.getString());

	}

}
