::This is bat file to generate the retrotranslated PowerMock-Legacy
::I know, I know, it should be an ANT file or a Maven Project. 
::Next version maybe.

::Assuming this is the right folder
cd C:\Users\Pablo\workspace\PowerMock-Legacy-01\Retrotranslator-1.2.9-bin

::Assuming the jar file is exported as c:\temp\PowerMock-Legacy.jar
java  -jar retrotranslator-transformer-1.2.9.jar -srcjar c:\temp\PowerMock-Legacy.jar -destjar c:\temp\PowerMock-Legacy-jvm14-13.0.2.jar  -target 1.4
pause