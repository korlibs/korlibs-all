#jdk_switcher use oraclejdk8

set -e

export PROJECT_DIR=$PWD
export ATOMICFU_DIR=$PROJECT_DIR/../kotlinx.atomicfu
export XCOROUTINES_DIR=$PROJECT_DIR/../kotlinx.coroutines

mkdir -p $ATOMICFU_DIR
pushd $ATOMICFU_DIR
	git clone https://github.com/korlibs/kotlinx.atomicfu.git $ATOMICFU_DIR
	git pull
	git checkout master
	./gradlew publishToMavenLocal -x dokka -x dokkaJavadoc -x test -x check
popd

mkdir -p $XCOROUTINES_DIR
pushd $XCOROUTINES_DIR
	git clone https://github.com/korlibs/kotlinx.coroutines.git $XCOROUTINES_DIR
	git pull
	git checkout master
	./gradlew publishToMavenLocal -x dokka -x dokkaJavadoc -x test -x check
popd

if [ "$kotlin_native_rev" != "" ]; then
	export KONAN_REPO=$PROJECT_DIR/../kotlin-native
    export build_kotlin_native=true
	mkdir -p $KONAN_REPO
	pushd $KONAN_REPO
		git clone https://github.com/JetBrains/kotlin-native.git $KONAN_REPO
		git pull
		git checkout $kotlin_native_rev
		./gradlew dependencies:update
		./gradlew dist distPlatformLibs
	popd

    ./gradlew -s check install -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
    pushd samples
		./gradlew -s :sample1-native:compileDebugMacos_x64KotlinNative -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
		./gradlew -s check -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
    popd
else
	./gradlew -s check install && pushd samples && ../gradlew -s check && popd
fi
