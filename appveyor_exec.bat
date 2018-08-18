set PROJECT_DIR=%CD%
set ATOMICFU_DIR=%PROJECT_DIR%\..\kotlinx.atomicfu
set XCOROUTINES_DIR=%PROJECT_DIR%\..\kotlinx.coroutines
set KONAN_BIN=C:\Users\appveyor\.konan\kotlin-native-macos-0.9-dev-3210\bin

mkdir %ATOMICFU_DIR%
pushd %ATOMICFU_DIR%
	git clone https://github.com/korlibs/kotlinx.atomicfu.git %ATOMICFU_DIR%
	git pull
	git checkout master
	call gradlew.bat publishToMavenLocal -x test -x check || exit /b
popd

mkdir %XCOROUTINES_DIR%
pushd %XCOROUTINES_DIR%
	git clone https://github.com/korlibs/kotlinx.coroutines.git %XCOROUTINES_DIR%
	git pull
	git checkout master
	call gradlew.bat publishToMavenLocal -x dokka -x dokkaJavadoc -x test -x check || exit /b
popd

REM kotlin-native-macos-0.9-dev-3210 doesn't have zlib on mingw yet
REM Fixed here: https://github.com/JetBrains/kotlin-native/commit/3ad52b8736482231d86d472e92c609a03d166cee
%KONAN_BIN%\bin\cinterop -def zlib.def -o zlib || exit /b
%KONAN_BIN%\bin\klib install zlib || exit /b

call gradlew.bat -s check install || exit /b
pushd samples
	call ..\gradlew.bat -s check || exit /b
popd
