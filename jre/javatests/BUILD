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
    javacopts = ["-Xep:SelfComparison:OFF"],
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
    javacopts = ["-Xep:SelfComparison:OFF"],
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

load("//third_party/java_src/gwt/build_def:java8_library.bzl", "java8_library")

java8_library(
    name = "jre_tests",
    testonly = 1,
    srcs = [
        ":emul_tests_java8_lib",
        ":emul_tests_lib_java_files",
        ":java_emul_internal",
    ],
    compile_deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/jdk:jdk8-google-v7-64/jre/lib/rt.jar",
        "//third_party/java/junit",
    ],
    import_deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

java_test(
    name = "AllEmulTest_Java8",
    size = "small",
    test_class = "com.google.gwt.emultest.EmulJava8Suite",
    runtime_deps = [
        ":jre_tests",
        "//third_party/java/junit",
    ],
)

load(":j2cl_multi_test.bzl", "j2cl_multi_test")

j2cl_multi_test(
    name = "BigDecimal",
    test_class = "com.google.gwt.emultest.BigDecimalTests",
)

j2cl_multi_test(
    name = "BigInteger",
    test_class = "com.google.gwt.emultest.BigIntegerTests",
)

j2cl_multi_test(
    name = "Collections",
    test_class = "com.google.gwt.emultest.CollectionsTests",
)

j2cl_multi_test(
    name = "Emul",
    _js_srcs = ["test_defines.js"],
    bootstrap_files = ["test_defines.js"],
    test_class = "com.google.gwt.emultest.EmulTests",
)

genrule(
    name = "test_defines",
    outs = ["test_defines.js"],
    cmd = "echo \"/** @define {string} */ var someConfigurationProperty='conf';\" > $@",
)
