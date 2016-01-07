# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_source_copy", "j2cl_source_copy")
load("/third_party/java_src/j2cl/jre/javatests/j2cl_multi_test", "j2cl_multi_test")
load("/third_party/java_src/j2cl/build_def/jsni_to_j2cl_converter", "jsni_to_j2cl_converter")

j2cl_source_copy(
    name = "emul_test_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:emul_test"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/test/",
    excludes = [
        "com/google/gwt/emultest/java8/util/OptionalTest.java",
        "com/google/gwt/emultest/EmulJava8Suite.java",
    ],
)

j2cl_source_copy(
    name = "emul_test_javaemul_copy",
    srcs = ["//third_party/java_src/gwt/svn/trunk/dev:emul"],
    base_strip_path = "third_party/java_src/gwt/svn/trunk/user/dev/core/super/",
)

# Since the tests refer to java.emul.internal
# we need these Java references to be valid, this is why we
# are exposing the extra java_library.
# The actual transpiled JavaScript code is coming from the
# underlying JRE dep of the j2cl_library rule.
java_library(
    name = "javaemul_interal_java",
    srcs = [":emul_test_javaemul_copy"],
)

jsni_to_j2cl_converter(
    name = "convert_jsni_emul_j2cl",
    testonly = 1,
    srcs = [
        ":emul_test_copy",
    ],
    excludes = [
        "com/google/gwt/testing/TestUtils.java",
        "java/lang/JsExceptionViolator.java",
        "java/math/BigIntegerViolator.java",
        "java/util/TreeMapViolator.java",
        "java/util/TreeMapViolator.java",
    ],
    deps = [
        ":emul_tests_lib",
        "//jre/java:JavaJre_java_library",
        "//junit/opensource/java:junit_emul_java_library",
    ],
)

TEST_SRCS = [
    "com/google/gwt/core/client/JavaScriptObject.java",
    "com/google/gwt/junit/DoNotRunWith.java",
    "com/google/gwt/junit/Platform.java",
    "com/google/gwt/junit/client/GWTTestCase.java",
    "com/google/gwt/junit/tools/GWTTestSuite.java",
    ":emul_test_copy",
]

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_library(
    name = "emul_tests_lib",
    testonly = 1,
    srcs = TEST_SRCS,
    deps = [
        ":javaemul_interal_java_library",
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = ["com/google/gwt/emultest/AllTests.java"],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        ":emul_tests_lib",
        ":javaemul_interal_java_library",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "BigDecimal",
    srcs = ["com/google/gwt/emultest/BigDecimalTests.java"],
    test_class = "com.google.gwt.emultest.BigDecimalTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "BigInteger",
    srcs = ["com/google/gwt/emultest/BigIntegerTests.java"],
    test_class = "com.google.gwt.emultest.BigIntegerTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "Collections",
    srcs = ["com/google/gwt/emultest/CollectionsTests.java"],
    test_class = "com.google.gwt.emultest.CollectionsTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)

j2cl_multi_test(
    name = "Emul",
    srcs = ["com/google/gwt/emultest/EmulTests.java"],
    test_class = "com.google.gwt.emultest.EmulTests",
    deps = [
        ":emul_tests_lib_j2cl",
        "//third_party/java/junit",
    ],
)
