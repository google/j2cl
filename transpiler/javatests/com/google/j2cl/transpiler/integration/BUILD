# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_java_library macro.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_test(
    name = "HelpFlagTest",
    srcs = ["HelpFlagTest.java"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "InvalidFlagTest",
    srcs = ["InvalidFlagTest.java"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "InvalidOptionTest",
    srcs = ["InvalidOptionTest.java"],
    data = glob(["invalidoutputoption/**/*"]),
    deps = [
        ":IntegrationTestCase",
        "//java/com/google/common/io",
    ],
)

java_test(
    name = "SyntaxErrorTest",
    srcs = ["SyntaxErrorTest.java"],
    data = glob(["syntaxerror/**/*"]),
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "MissingDepTest",
    srcs = ["MissingDepTest.java"],
    data = glob(["missingdep/**/*"]),
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "NativeSourcesErrorTest",
    srcs = ["NativeSourcesErrorTest.java"],
    data = glob(["nativesourceserror/**/*"]) + ["//jre/java:libJavaJre.jar"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "OutputTest",
    srcs = ["OutputTest.java"],
    data = glob(["output/**/*"]) + ["//jre/java:libJavaJre.jar"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "ValidOptionsTest",
    srcs = ["ValidOptionsTest.java"],
    data = glob(["validoptions/**/*"]),
    deps = [":IntegrationTestCase"],
)

java_library(
    name = "IntegrationTestCase",
    testonly = 1,
    srcs = ["IntegrationTestCase.java"],
    data = ["//third_party/java_src/j2cl"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//java/com/google/devtools/build/runtime:runfiles",
        "//third_party/java/junit",
    ],
)
