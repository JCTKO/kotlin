package kotlin.test.tests

import kotlin.test.*
import kotlin.native.internal.test.*

@Test
fun test() {
    println("test")
}

fun main(args: Array<String>) {
    println("Custom main")
    testLauncherEntryPoint(args)
}