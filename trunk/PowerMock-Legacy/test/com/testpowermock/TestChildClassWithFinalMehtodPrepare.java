package com.testpowermock;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestChildClassWithFinalMehtodPrepare implements IPrepareForTest{

	@Override
	public Class<?>[] classesToPrepare() {
		return new Class<?>[]{ChildClassWithFinalMethod.class};
	}
	
	@Override
	public String[] fullyQualifiedNamesToPrepare() {		
		return null;
	}
	
	@Test	
	public void testGetStringInChild() {
		ChildClassWithFinalMethod childClassWithFinalMethod = PowerMockito.mock(ChildClassWithFinalMethod.class);
		PowerMockito.when(childClassWithFinalMethod.getStringInChild()).thenReturn("NEW_STRING_IN_CHILD");

		assertEquals("NEW_STRING_IN_CHILD", childClassWithFinalMethod.getStringInChild());
	}

	@Test
	// I have only prepared ChildClassWithFinalMethod, but PrepareForTest 
	//also prepares superclass ClassWithFinalMethod
	public void testGetStringInSuper() {
		ClassWithFinalMethod classWithFinalMehtod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMehtod.getString()).thenReturn("NEW_STRING");

		assertEquals("NEW_STRING", classWithFinalMehtod.getString() );
	}


	


}
