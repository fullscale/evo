@echo off

SETLOCAL

if NOT DEFINED JAVA_HOME goto err

set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%..") do set C9_HOME=%%~dpfI


REM ***** JAVA options *****

if "%C9_MIN_MEM%" == "" (
set C9_MIN_MEM=256m
)

if "%C9_MAX_MEM%" == "" (
set C9_MAX_MEM=1g
)

if NOT "%C9_HEAP_SIZE%" == "" (
set C9_MIN_MEM=%C9_HEAP_SIZE%
set C9_MAX_MEM=%C9_HEAP_SIZE%
)

set JAVA_OPTS=%JAVA_OPTS% -Xms%C9_MIN_MEM% -Xmx%C9_MAX_MEM%

if NOT "%C9_HEAP_NEWSIZE%" == "" (
set JAVA_OPTS=%JAVA_OPTS% -Xmn%C9_HEAP_NEWSIZE%
)

if NOT "%C9_DIRECT_SIZE%" == "" (
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxDirectMemorySize=%C9_DIRECT_SIZE%
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
REM JAVA_OPTS=%JAVA_OPTS% -Xloggc:%C9_HOME%\logs\gc.log

REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
REM The path to the heap dump location, note directory must exists and have enough
REM space for a full heap dump.
REM set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=%C9_HOME%\logs\heapdump.hprof

set C9_CLASSPATH=%C9_CLASSPATH%;%C9_HOME%/etc;%C9_HOME%/etc/security;%C9_HOME%/lib/${project.build.finalName}.jar;%C9_HOME%/lib/*;%C9_HOME%/lib/sigar/*
set C9_PARAMS=-Dc9.home="%C9_HOME%"

"%JAVA_HOME%\bin\java" %JAVA_OPTS% %C9_JAVA_OPTS% %C9_PARAMS% %* -cp "%C9_CLASSPATH%" "co.diji.cloud9.Cloud9"
goto finally


:err
echo JAVA_HOME environment variable must be set!
pause


:finally

ENDLOCAL
