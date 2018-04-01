# KBigNum

[![Build Status](https://travis-ci.org/soywiz/kbignum.svg?branch=master)](https://travis-ci.org/soywiz/kbignum)

## BigInt

Provides a portable implementation of an arbitrary sized Big Integer in Kotlin Common.

Exposes `expect` and `actual` for targets including a native BigInteger library,
and uses the common pure kotlin implementation in other targets.

## BigDecimal

Using BigInt implements a BigDecimal class.