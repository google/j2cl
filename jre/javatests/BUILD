# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    testonly = 1,
    generate_build_test = False,
    javacopts = [
        "-Xep:CheckReturnValue:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
    ],
    mirrored_files = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    deps = [
        "//jre/java:gwt-jsinterop-annotations",
        "//third_party/java/junit:junit-j2cl",
    ],
)

j2cl_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = [
        # Uses a method on Throwable that is not present in Java:
        # Throwable#getBackingJsObject
        "javaemul/internal/ConsoleLogger.java",
        # Emulated charset contains classes extending from
        # java.nio.charset.Charset that are not implementing all Java methods.
        # This is okay in the emulated version since emulated
        # java.nio.charset.Charset does not have these methods.
        "javaemul/internal/EmulatedCharset.java",
    ],
)

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = [
        ":emul_tests_lib_java_files",
        ":java_emul_internal",
    ],
    javacopts = [
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
        "-Xep:ReturnValueIgnored:OFF",
    ],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

load(":j2cl_multi_test.bzl", "j2cl_multi_test")

j2cl_multi_test(
    name = "BigDecimal",
    shard_count = 5,
    test_class = "com.google.gwt.emultest.BigDecimalTests",
)

j2cl_multi_test(
    name = "BigInteger",
    shard_count = 8,
    test_class = "com.google.gwt.emultest.BigIntegerTests",
)

j2cl_multi_test(
    name = "Collections",
    shard_count = 16,
    test_class = "com.google.gwt.emultest.CollectionsTests",
)

j2cl_multi_test(
    name = "Emul",
    _js_srcs = ["test_defines.js"],
    bootstrap_files = ["test_defines.js"],
    shard_count = 16,
    test_class = "com.google.gwt.emultest.EmulTests",
)

j2cl_multi_test(
    name = "EmulJava8",
    shard_count = 8,
    test_class = "com.google.gwt.emultest.EmulJava8Tests",
)

genrule(
    name = "test_defines",
    outs = ["test_defines.js"],
    cmd = "echo \"/** @define {string} */ var someConfigurationProperty='conf';\" > $@",
)
