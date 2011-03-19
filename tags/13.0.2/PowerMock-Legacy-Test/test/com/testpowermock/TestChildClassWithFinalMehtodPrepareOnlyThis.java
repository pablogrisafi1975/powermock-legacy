package com.testpowermock;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mockito.exceptions.misusing.MissingMethodInvocationException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareOnlyThisForTest;
import org.powermock.modules.junit3.PowerMockSuite;

public class TestChildClassWithFinalMehtodPrepareOnlyThis extends TestCase implements IPrepareOnlyThisForTest {

	public Class[] classesToPrepareOnlyThis() {
		return new Class[] { ChildClassWithFinalMethod.class };
	}

	public String[] fullyQualifiedNamesToPrepareOnlyThis() {
		return null;
	}
	
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}
	

	// PrepareOnlyThis should not let you change super class
	public void testGetStringInSuperWithOnlyThis() {
		Exception ex = null;
		try {
			ClassWithFinalMethod classWithFinalMehtod = (ClassWithFinalMethod) PowerMockito.mock(ClassWithFinalMethod.class);
			PowerMockito.when(classWithFinalMehtod.getString()).thenReturn("NEW_STRING");
		} catch (MissingMethodInvocationException mmie) {
			ex = mmie;
		}
		assertNotNull(ex);
		assertEquals(MissingMethodInvocationException.class, ex.getClass());
	}

}
