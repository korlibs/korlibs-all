#jdk_switcher use oraclejdk8

set -e

export PROJECT_DIR=$PWD
export ATOMICFU_DIR=$PROJECT_DIR/../kotlinx.atomicfu
export XCOROUTINES_DIR=$PROJECT_DIR/../kotlinx.coroutines

test -d $ATOMICFU_DIR || mkdir -p $ATOMICFU_DIR
pushd $ATOMICFU_DIR
	test -d $ATOMICFU_DIR/.git || git clone https://github.com/korlibs/kotlinx.atomicfu.git $ATOMICFU_DIR
	git pull
	git checkout master
	echo "gradle.taskGraph.whenReady { graph -> graph.allTasks.findAll { it.name ==~ /(.*ReleaseMacos.*|.*Ios.*)/ }*.enabled = false }" >> build.gradle
	./gradlew publishToMavenLocal -x test -x check
popd

test -d $XCOROUTINES_DIR || mkdir -p $XCOROUTINES_DIR
pushd $XCOROUTINES_DIR
	test -d $XCOROUTINES_DIR/.git || git clone https://github.com/korlibs/kotlinx.coroutines.git $XCOROUTINES_DIR
	git pull
	git checkout master
	echo "gradle.taskGraph.whenReady { graph -> graph.allTasks.findAll { it.name ==~ /(.*ReleaseMacos.*|.*Ios.*)/ }*.enabled = false }" >> build.gradle
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
		#./gradlew -s :sample1-native:compileDebugMacos_x64KotlinNative -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
		#./gradlew -s check -x compileReleaseKotlinNative -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
    popd
else
	./gradlew -s check install
	pushd samples
		echo Not bulding sample, because dragonbones is not built because out of memory on CI
		#../gradlew -s check
	popd
fi
