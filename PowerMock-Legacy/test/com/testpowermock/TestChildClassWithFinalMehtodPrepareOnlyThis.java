package com.testpowermock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.exceptions.misusing.MissingMethodInvocationException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestChildClassWithFinalMehtodPrepareOnlyThis implements IPrepareOnlyThisForTest {

	@Override
	public Class<?>[] classesToPrepareOnlyThis() {
		return new Class<?>[] { ChildClassWithFinalMethod.class };
	}

	@Override
	public String[] fullyQualifiedNamesToPrepareOnlyThis() {
		return null;
	}

	@Test
	// PrepareOnlyThis should not let you change super class
	public void testGetStringInSuperWithOnlyThis() {
		Exception ex = null;
		try {
			ClassWithFinalMethod classWithFinalMehtod = PowerMockito.mock(ClassWithFinalMethod.class);
			PowerMockito.when(classWithFinalMehtod.getString()).thenReturn("NEW_STRING");
		} catch (MissingMethodInvocationException mmie) {
			ex = mmie;
		}
		assertNotNull(ex);
		assertEquals(MissingMethodInvocationException.class, ex.getClass());
	}

}
