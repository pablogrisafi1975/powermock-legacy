package com.testpowermock;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareForTest;
import org.powermock.modules.junit3.PowerMockSuite;

public class TestChildClassWithFinalMehtodPrepare extends TestCase implements IPrepareForTest{

	public Class[] classesToPrepare() {
		return new Class[]{ChildClassWithFinalMethod.class};
	}
	
	public String[] fullyQualifiedNamesToPrepare() {		
		return null;
	}

	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}

	public void testGetStringInChild() {
		ChildClassWithFinalMethod childClassWithFinalMethod = (ChildClassWithFinalMethod) PowerMockito.mock(ChildClassWithFinalMethod.class);
		PowerMockito.when(childClassWithFinalMethod.getStringInChild()).thenReturn("NEW_STRING_IN_CHILD");

		assertEquals("NEW_STRING_IN_CHILD", childClassWithFinalMethod.getStringInChild());
	}

	// I have only prepared ChildClassWithFinalMethod, but PrepareForTest 
	//also prepares superclass ClassWithFinalMethod
	public void testGetStringInSuper() {
		ClassWithFinalMethod classWithFinalMehtod = (ClassWithFinalMethod) PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMehtod.getString()).thenReturn("NEW_STRING");

		assertEquals("NEW_STRING", classWithFinalMehtod.getString() );
	}


	


}
