package com.testpowermock;

public class ChildClassWithFinalMethod extends ClassWithFinalMethod {
	public final String getStringInChild(){
		return "ORIGINAL_STRING_IN_CHILD";
	}

}
