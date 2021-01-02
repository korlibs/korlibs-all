<h1 align="center">Korlibs</h1>

<p align="center">Multimodule including all the libraries that are part of the korlibs.</p>

<!-- SUPPORT -->

<h2 align="center">Support this project</h2>

<p align="center">
If you like the project, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a sponsor ★</a>,<br />
in addition to ensure the continuity of the project, you will get exclusive content.
</p>

<!-- /SUPPORT -->

This repository is for core-devs only. If you want to contribute to any lib,
please download the `korge-next` repository and make a PR there.
We will manually merge your changes to each repo on the next release.

Remember to download the modules:
```bash
git submodule update --init --recursive
```

### Korlibs without dependencies (First Layer)

* [klock](https://github.com/korlibs/klock) - Date and Time Library
* [kds](https://github.com/korlibs/kds) - Data Structures Library
* [kmem](https://github.com/korlibs/kmem) - Fast Memory and bit utilities
* [korinject](https://github.com/korlibs/korinject) - Asynchronous dependency injector
* [krypto](https://github.com/korlibs/krypto) - Cryptographic library
* [klogger](https://github.com/korlibs/klogger) - Logger library
* [kbignum](https://github.com/korlibs/kbignum) - BigInteger and BigDecimal library for common

### Depending on kds and klock (Second layer)

* [korio](https://github.com/korlibs/korio) - I/O Libraries
* [korma](https://github.com/korlibs/korma) - Mathematics library mostly focused on 2d and 3d geometry and algebra

### Depending on korio and korma (Third layer)

* [korim](https://github.com/korlibs/korim) - Imaging and Vector Library
* [korau](https://github.com/korlibs/korau) - Audio Library

### Depending on korim (Fourth layer)

* [kgl/korag/korgw/korui](https://github.com/korlibs/korui) - Accelerated Grahphics, UI and Game Window

### Depending on everything else (Fifth layer)

* [korge](https://github.com/korlibs/korge) - 2D and 3D Game Engine

### Others

* [kbox2d](https://github.com/korlibs/kbox2d) - Box2D port of JBox2D/Box2D physics engine
* [kortemplate](https://github.com/korlibs/kortemplate) - Gradle Template used for all these projects (buildSrc/build.gradle and settings.gradle)
