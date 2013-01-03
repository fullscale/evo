@echo off

SETLOCAL

if NOT DEFINED JAVA_HOME goto err

set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%..") do set EVO_HOME=%%~dpfI


REM ***** JAVA options *****

if "%EVO_MIN_MEM%" == "" (
set EVO_MIN_MEM=256m
)

if "%EVO_MAX_MEM%" == "" (
set EVO_MAX_MEM=1g
)

if NOT "%EVO_HEAP_SIZE%" == "" (
set EVO_MIN_MEM=%EVO_HEAP_SIZE%
set EVO_MAX_MEM=%EVO_HEAP_SIZE%
)

set JAVA_OPTS=%JAVA_OPTS% -Xms%EVO_MIN_MEM% -Xmx%EVO_MAX_MEM%

if NOT "%EVO_HEAP_NEWSIZE%" == "" (
set JAVA_OPTS=%JAVA_OPTS% -Xmn%EVO_HEAP_NEWSIZE%
)

if NOT "%EVO_DIRECT_SIZE%" == "" (
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxDirectMemorySize=%EVO_DIRECT_SIZE%
)

set JAVA_OPTS=%JAVA_OPTS% -Xss256k

REM Enable aggressive optimizations in the JVM
REM    - Disabled by default as it might cause the JVM to crash
REM set JAVA_OPTS=%JAVA_OPTS% -XX:+AggressiveOpts

set JAVA_OPTS=%JAVA_OPTS% -XX:+UseParNewGC
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseConcMarkSweepGC

set JAVA_OPTS=%JAVA_OPTS% -XX:CMSInitiatingOccupancyFraction=75
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseCMSInitiatingOccupancyOnly

REM When running under Java 7
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseCondCardMark

REM GC logging options -- uncomment to enable
REM JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDetails
REM JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCTimeStamps
REM JAVA_OPTS=%JAVA_OPTS% -XX:+PrintClassHistogram
REM JAVA_OPTS=%JAVA_OPTS% -XX:+PrintTenuringDistribution
REM JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCApplicationStoppedTime
REM JAVA_OPTS=%JAVA_OPTS% -Xloggc:%EVO_HOME%\logs\gc.log

REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
REM The path to the heap dump location, note directory must exists and have enough
REM space for a full heap dump.
REM set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=%EVO_HOME%\logs\heapdump.hprof

set JAVA_OPTS=%JAVA_OPTS% -Xbootclasspath/p:%EVO_HOME%/lib/boot/npn-boot-1.1.1.v20121030.jar

set EVO_CLASSPATH=%EVO_CLASSPATH%;%EVO_HOME%/etc;%EVO_HOME%/etc/security;%EVO_HOME%/lib/${project.build.finalName}.jar;%EVO_HOME%/lib/*;%EVO_HOME%/lib/sigar/*
set EVO_PARAMS=-Dc9.home="%EVO_HOME%"

"%JAVA_HOME%\bin\java" %JAVA_OPTS% %EVO_JAVA_OPTS% %EVO_PARAMS% %* -cp "%EVO_CLASSPATH%" "co.fs.evo.Evo"
goto finally


:err
echo JAVA_HOME environment variable must be set!
pause


:finally

ENDLOCAL
