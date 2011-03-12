package com.testpowermock;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.interfaces.IPrepareForTest;
import org.powermock.core.classloader.interfaces.IPrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestAllAnnoyingClassesMixingInterfaces implements IPrepareForTest, IPrepareOnlyThisForTest {


	@Override
	public Class<?>[] classesToPrepare() {
		return new Class<?>[] { ClassWithFinalMethod.class, ClassWithStaticFinalMethod.class , ClassWithStaticMethod.class, FinalClassWithFinalMethod.class};
	}
	
	@Override
	public String[] fullyQualifiedNamesToPrepare() {		
		return null;
	}
	
	
	@Override
	public Class<?>[] classesToPrepareOnlyThis() {
		return new Class<?>[] { ClassWithFinalMethod.class, ClassWithStaticFinalMethod.class , ClassWithStaticMethod.class, FinalClassWithFinalMethod.class};
	}
	
	@Override
	public String[] fullyQualifiedNamesToPrepareOnlyThis() {		
		return null;
	}
	


	@Test
	public void testClassWithFinalMethod_GetString() {
		ClassWithFinalMethod classWithFinalMethod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}
	@Test
	public void testClassWithStaticFinalMethod_GetString() {
		PowerMockito.mockStatic(ClassWithStaticFinalMethod.class);
		PowerMockito.when(ClassWithStaticFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticFinalMethod.getString());

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
