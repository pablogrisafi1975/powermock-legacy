![https://raw.githubusercontent.com/pablogrisafi1975/powermock-legacy/master/powermock-legacy.png](https://raw.githubusercontent.com/pablogrisafi1975/powermock-legacy/master/powermock-legacy.png)
# Introduction #
Even now in 2011 there are lots of legacy projects that require Java 1.4 (particularly in big conservative businesses), and most of this projects were not designed with testability in mind, so they tend to use and abuse static (and sometimes final) methods. These methods are not easy to mock with standard tools like mockito. [PowerMock](http://code.google.com/p/powermock/) is the solution, but it needs Java 1.5 or more.
Well, if that sounds familiar to you, here is a solution.
[PowerMock-Legacy](https://github.com/pablogrisafi1975/powermock-legacy/) is a fork of [PowerMock](http://code.google.com/p/powermock/) 1.3 that can help you mock objects and classes in Java 1.4.


---


# Requirements #

  * Java 1.4 (I assume you already installed it)
  * JUnit 3.8.1 (The last one that works with jvm 1.4)
  * Mockito 1.8.0 for jvm 1.4 (The one James Carr compiles)
  * RetroTranslator Runtime 1.2.9
  * Javassist 3.11.0.GA (I think it is needed by RetroTranslator Runtime)
And of course, PowerMock-Legacy, last version ;-)

You can download all these files in one zip file in the Download section.

---

# News #
**2011-08-18:** Good news! (for my ego at least):  Jan & Johan from JayWay, the original authors for PowerMock have been kind enough to link to this project. Yeah, I'm the greatest man alive!

**2011-08-28:** Bad news. I'm officially leaving the project. I don't need it anymore (working in java 1.5 for now on, oh yeah that's how I like it!), and I don't have either  time or will to go on with this. Since PowerMock-Legacy had only 11 downloads, I assume there's not going to be a riot about that. If you want to use it, go on and download, it does work. If you want to improve it by fixing bugs and/or extending functionality, fork it. Just drop me a line if you need any help understanding the mess

**2015-03-18:** Indiferent news. Moved to GitHub. There were 112 downloads of the full version!
---

# Usage #

Remember to put all needed jars in your buildpath!
Notation is not very nice, but works. Imagine you have a nice class like

```
package com.testpowermock;

public class ClassWithFinalMethod {
	public final String getString() {
		return "ORIGINAL_STRING";
	}
}
```

In a beautiful Java 1.5 world you can write something like
```
@RunWith(PowerMockRunner.class) //Here you are saying "Use PowerMock Runner to run this class"
@PrepareForTest(ClassWithFinalMethod.class)//Here you are saying "Prepare ClassWithFinalMethod to be mocked with PowerMock"
public class TestClassWithFinalMethodClassic {

	@Test //Here you are saying "This is a test"
	public void getString() {
		ClassWithFinalMethod classWithFinalMethod = PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}
}

```

In our sad Java 1.4 world you'll need be more verbose
```
public class TestClassWithFinalMethod 
    extends TestCase //this is the old JUnit 3.8.1 way to say "this is a test class" and gives you all the assert methods
    implements IPrepareForTest //This interface says "We will need PowerMock-Legacy"{
	//This method belongs to the interface IPrepareForTest and must return the list
        //of classes that needs to be prepared
	public Class[] classesToPrepare() {
		return new Class[] { ClassWithFinalMethod.class};
	}

        //This method also belongs to the interface IPrepareForTest and can also be used
        //to give classes and even namespaces that need to be prepared. But since we don't
        //need it this time, we can simple return null
	public String[] fullyQualifiedNamesToPrepare() {		
		return null;
	}	
	
        //And this *static* method is the way to tell JUnit 3.8.1 to use PowerMock to run this test
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestAllAnnoyingClasses.class });
	}
	//And your test methods must start with "test", obviously
	public void testGetString() {
                //And this extra cast is needed because of the lack of generics
		ClassWithFinalMethod classWithFinalMethod = (ClassWithFinalMethod) PowerMockito.mock(ClassWithFinalMethod.class);
		PowerMockito.when(classWithFinalMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", classWithFinalMethod.getString());

	}
}
```

As you can see, it is more verbose, but not **that** verbose. You will pay a price to use this old version of Java. Don't blame it on me.

Another example for a static method. Let's say you have a class
```
public class ClassWithStaticMethod {
	public static String getString() {
		return "ORIGINAL_STRING";
	}
}
```
You know what to do in Java 1.5. In Java 1.4 you need to write this:
```
public class TestClassWithStaticMethod  extends TestCase implements IPrepareForTest {
	
	public Class[] classesToPrepare() {
		return new Class[] { ClassWithStaticMethod.class};
	}
	public String[] fullyQualifiedNamesToPrepare() {		
		return null;
	}
	
	public static TestSuite suite() throws Exception {
		return new PowerMockSuite(new Class[] { TestClassWithStaticMethod.class });
	}
	
	public void testGetString() {
		PowerMockito.mockStatic(ClassWithStaticMethod.class);
		PowerMockito.when(ClassWithStaticMethod.getString()).thenReturn("NEW STRING");

		assertEquals("NEW STRING", ClassWithStaticMethod.getString());
	}
}
```

The general strategy is
  1. Include a public static method **suite** to tell JUnit to use PowerMock-legacy to run test
  1. Instead of using class level annotations use interfaces to specify PowerMock actions
  1. Implement methods that belong to interfaces with the same information you should use in the corresponding annotation. But since interfaces do not have default values, return null when you don't need to use a particular method

Am I making myself clear? Check for other examples [here](http://code.google.com/p/powermock-legacy/source/browse/#svn%2Ftrunk%2FPowerMock-Legacy-Test%2Ftest%2Fcom%2Ftestpowermock)


---

# Limitations, Bugs, Known Issues #

  * Only Mockito works, not EasyMock or TestNG
  * A limited (but useful) subset of PowerMock is available righ now. Only PrepareForTest, PrepareOnlyThisForTest, PrepareEverythingForTest and PowerMockIgnore are working.
  * Even worst, interfaces are always class level, so you can't have in the same class two different tests, one that needs original class A and mocked class B, and another test that need mocked class A and original class B. If this is what you need, separate this tests in two different classes
  * Notation sucks

---

# Current status #
| **annotation** | **Interface** | **status** |
|:---------------|:--------------|:-----------|
| @PrepareForTest | IPrepareForTest | Works, but only at class level |
| @PrepareOnlyThisForTest| IPrepareOnlyThisForTest| Works, but only at class level |
| @PrepareEverythingForTest| IPrepareEverythingForTest| Works, but only at class level |
| @PowerMockIgnore| IPowerMockIgnore| Works, but only at class level |
| @SuppressStaticInitializationFor| _Not done_| Never needed it, but I think it is possible to retro-migrate |
| @PowerMockListener| _Not done_| Never needed it, but I think it is possible to retro-migrate |
| @MockPolicy| _Not done_| Never needed it, but I think it is possible to retro-migrate |
| @Mock| _Not done_| I don't think it is possible to retro-migrate, because it is a field level annotation |


---

# TODO list #
  * Make EasyMock and TestNG work
  * Implement more annotations as interfaces
  * Create some way to use a PowerMocked class only for specific methods inside a class
  * Improve the website
  * Use a newer version of everything
  * Use ANT and/or Maven


---


# FAQ #
## Why did you change annotations to interfaces? ##
Annotations does not exist in Java 1.4
## Wouldn't it be better to use a config file? ##
Probably, buy I like my tests to be self contained
## What does the version number mean? Did you really made 13 versions? ##
Not at all. The 13 means this is a fork of PowerMock version 13. The 13.0.1 indicates the first working version of the fork, 13.0.2 the second one, and so on.
## How is this project organized? ##
There are two Eclipse projects for each version called PowerMock-Legacy and PowerMock-Legacy-Test. PowerMock-Legacy is a Java 1.6 project I use to create a system that works both with annotations and interfaces. When I manage to make it work and pass some tests, I export it as jar file called C:\temp\PowerMock-Legacy.jar. Then I use RetroTranslator to create a Java 1.4 version called PowerMock-Legacy-jvm14-13.0.2
Once I get that file I copy it into PowerMock-Legacy-Test. This is a Java 1.4 project, create some tests and check what happens.
## Why do you use Eclipse and even batch files in your project? Wouldn't it be better to use some IDE-OS-agnostic system like ANT or Maven? ##
You are right, but I needed to make it work fast, and I work in Eclipse/Windows. So I was lazy. Next time I'll make a real decent project

---

Please, don't go on reading...the next part is way too incomplete...just try and see if PowerMock-Legacy is what you need.


---


# Blast to the past #
If you need to make unit tests with Java 1.4, you must go back and get JUnit 3.8.1. Don't even dream about static imports and stuff like that.But that's not all.

[PowerMock](http://code.google.com/p/powermock/) works like an extension of [EasyMock](http://easymock.org/) and [Mockito](http://mockito.org/) that allows you to mock unmockable classes and methods, like final or static
For my own personal needs, I only cared about making [Mockito](http://mockito.org/) work.
Unfortunately, like any other decent software, [Mockito](http://mockito.org/) has abandon support for Java 1.4. Luckily, there is a guy called [James Carr](http://blog.james-carr.org) who has pretty much the same problem that me, and he had made a retro port of [Mockito](http://mockito.org/) to Java 1.4.
He had been nice enough to put share it [here](http://blog.james-carr.org/2009/10/21/mockito-for-jvm-14-released/), and now [Mockito](http://mockito.org/) people is offering it [here](http://code.google.com/p/mockito/wiki/MockitoForJvm14). He managed to do that using
[Retrotranslator](http://retrotranslator.sourceforge.net), an incredible tool that modifies Java 1.5 bytecode (not source) to make compatible with Java 1.4, and also uses a runtime jar to provide support for some classes that are not present in Java 1.4 VM, like AtomicInteger or Callable.
I use [Retrotranslator](http://retrotranslator.sourceforge.net) in this project too.

Every version of [PowerMock](http://code.google.com/p/powermock/) is heavily linked to some version of [Mockito](http://mockito.org/),  so in order to use [Mockito](http://mockito.org/) 1.8.0 I needed to go back and get [PowerMock](http://code.google.com/p/powermock/) 1.3.

So, to summarize, to **create** [PowerMock-legacy](http://code.google.com/p/powermock-legacy/) I needed
  * Java 1.4 (I assume you already installed it)
  * JUnit 3.8.1 ([download](http://sourceforge.net/projects/junit/files/junit/3.8.1/))
  * Mockito 1.8.0 ([download](http://code.google.com/p/mockito/wiki/MockitoForJvm14))
  * PowerMock 1.3 src ([download](http://code.google.com/p/powermock/downloads/list))
  * RetroTranslator ([Retrotranslator](http://retrotranslator.sourceforge.net) download)
Don't rush into downloading everything right now, to **use** [PowerMock-legacy](http://code.google.com/p/powermock-legacy/) you don't need the original source code of PowerMock 1.3 , and even if you want to build it yourself, that's what's SVN is for.


# The curse of annotations #
[Retrotranslator](http://retrotranslator.sourceforge.net) makes an excellent job, but in order to use [PowerMock](http://code.google.com/p/powermock/) you need to use annotations, and there is no way of writing

```
@PrepareForTest({ ClassWithStatic.class })
public class TestClassWithStatic {
    public void testGetString() {
    ....
    }
}
```

All other components have a past in Java 1.4 and have a way to expose its features in a pre-annotation world. But [PowerMock](http://code.google.com/p/powermock/) relays in annotations to know what classes should be loaded using its own custom classloader.
The older way to mark a class in Java 1.4 was to use an interface.(As a matter of fact, an annotation is a strange kind of an interface). So, instead of writing an annotation in every class we need to prepare for test, we can make it implement an interface like this one

```
public class TestAnnoyingClasses extends TestCase implements IPrepareForTest {
   ...
}
```
