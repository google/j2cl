# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    generate_build_test = False,
    javacopts = [
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
    ],
    mirrored_files = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    deps = [
        "//jre/java:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

j2cl_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = [
        "javaemul/internal/ConsoleLogger.java",
        "javaemul/internal/EmulatedCharset.java",
        "javaemul/internal/HashCodes.java",  # relies on java8 only APIs
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
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
    ],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

j2cl_source_copy(
    name = "emul_tests_java8_lib",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java8_emul_test"],
)

java_library(
    name = "jre_tests",
    testonly = 1,
    srcs = [
        ":emul_tests_java8_lib",
        ":emul_tests_lib_java_files",
        ":java_emul_internal",
    ],
    javacopts = [
        "-Xep:SelfEquals:OFF",
    ],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

java_test(
    name = "AllEmulTest_Java8",
    size = "small",
    javacopts = [
        "-Xep:SelfEquals:OFF",
    ],
    test_class = "com.google.gwt.emultest.EmulJava8Suite",
    runtime_deps = [
        ":jre_tests",
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

genrule(
    name = "test_defines",
    outs = ["test_defines.js"],
    cmd = "echo \"/** @define {string} */ var someConfigurationProperty='conf';\" > $@",
)

load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")

# This target uses a special version of the transpiler that compiles code
# twice within the transpiler.
# This is intended to detect problems with static state in the transpiler.
j2cl_transpile(
    name = "jre_transpiled_twice",
    srcs = ["//jre/java:jre_hidden_java_files"],
    native_srcs_zips = ["//jre/java:jre_native_zips"],
    transpiler = ":RerunningJ2clTranspiler",
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

java_binary(
    name = "RerunningJ2clTranspiler",
    main_class = "com.google.j2cl.transpiler.RerunningJ2clTranspiler",
    runtime_deps = ["//transpiler/javatests/com/google/j2cl/transpiler:rerunning_transpiler"],
)

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "jre_transpiling_twice_test",
    targets = [
        ":jre_transpiled_twice",
    ],
)
