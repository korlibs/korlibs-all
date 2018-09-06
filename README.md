Korlibs
=======

[![Build Status](https://travis-ci.org/korlibs/korlibs.svg?branch=master)](https://travis-ci.org/korlibs/korlibs)
[![Build status](https://ci.appveyor.com/api/projects/status/15m7n3axie8h812s?svg=true)](https://ci.appveyor.com/project/soywiz/korlibs)
[![Download]([https://api.bintray.com/packages/soywiz/soywiz/korlibs/images/download.svg)](https://bintray.com/soywiz/soywiz/korlibs/_latestVersion)

To make development, evolution and deployment easier, all the korlibs libraries are in this single mono-repository.

Once relevant functionality is available as standard Kotlin libraries, I will deprecate the specific korlibs libraries
and will provide a relevant migration utilities for them.

### Useful links

* Documentation: <https://korlibs.soywiz.com/>
* Blog: <https://soywiz.com/korlibs/>

### Projects using Korlibs

* <https://github.com/mmo-poc/mmo-poc>
* <https://github.com/kpspemu/kpspemu>

### Extra

To test bleeding-edge kotlin-native:

```
export KONAN_REPO=$PWD/../kotlin-native
export build_kotlin_native=true

#pushd $KONAN_REPO && git pull && ./gradlew clean dependencies:update dist distPlatformLibs && popd

./gradlew install -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin

pushd samples
	./gradlew :sample1-native:compileDebugMacos_x64KotlinNative -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
popd
```

