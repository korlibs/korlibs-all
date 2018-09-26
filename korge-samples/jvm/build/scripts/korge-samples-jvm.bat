@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  korge-samples-jvm startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and KORGE_SAMPLES_JVM_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\korge-samples-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korge-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korau-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korui-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korag-opengl-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korag-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kgl-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korim-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korio-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korinject-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\klock-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kmem-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korma-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kds-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\klogger-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kotlinx-coroutines-core-0.26.0-rc13.jar;%APP_HOME%\lib\korlibstd-jvm-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\atomicfu-0.11.7-rc.jar;%APP_HOME%\lib\kotlin-stdlib-1.3.0-rc-57.jar;%APP_HOME%\lib\korge-samples-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korge-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korau-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korui-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korag-opengl-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korag-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kgl-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korim-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korio-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korinject-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\klock-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kmem-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\korma-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kds-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\klogger-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\kotlinx-coroutines-core-common-0.26.0-rc13.jar;%APP_HOME%\lib\korlibstd-common-0.47.0-SNAPSHOT.jar;%APP_HOME%\lib\atomicfu-common-0.11.7-rc.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.3.0-rc-57.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\Java-WebSocket-1.3.8.jar;%APP_HOME%\lib\gluegen-rt-2.3.2.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-android-aarch64.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-android-armv6.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-linux-amd64.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-linux-armv6.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-linux-armv6hf.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-linux-i586.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-macosx-universal.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-solaris-amd64.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-solaris-i586.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-windows-amd64.jar;%APP_HOME%\lib\gluegen-rt-2.3.2-natives-windows-i586.jar;%APP_HOME%\lib\jogl-all-2.3.2.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-android-aarch64.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-android-armv6.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-linux-amd64.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-linux-armv6.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-linux-armv6hf.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-linux-i586.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-macosx-universal.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-solaris-amd64.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-solaris-i586.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-windows-amd64.jar;%APP_HOME%\lib\jogl-all-2.3.2-natives-windows-i586.jar

@rem Execute korge-samples-jvm
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %KORGE_SAMPLES_JVM_OPTS%  -classpath "%CLASSPATH%" Sample1 %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable KORGE_SAMPLES_JVM_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%KORGE_SAMPLES_JVM_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
