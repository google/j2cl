load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "kotlin_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest:__pkg__"],
    licenses = ["notice"],
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterTest",
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    platforms = [
        "CLOSURE",
    ],
    deps = [":BeforeAndAfterTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ChildWithNewMethodsTest",
    platforms = [
        "CLOSURE",
    ],
    deps = [":ChildTest-lib"],
)

j2cl_test_integration_test_data(
    name = "GrandChildWithNewMethodsTest",
    platforms = [
        "CLOSURE",
    ],
    deps = [":ChildWithNewMethodsTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ExpectedExceptionTest",
    platforms = [
        "CLOSURE",
    ],
)

kotlin_and_j2cl_library(
    name = "IgnoreTestParent",
    srcs = ["IgnoreTestParent.kt"],
)

j2cl_test_integration_test_data(
    name = "IgnoreTest",
    platforms = [
        "CLOSURE",
    ],
    deps = [":IgnoreTestParent"],
)

kotlin_and_j2cl_library(
    name = "MethodOrderingTestParent",
    srcs = ["MethodOrderingTestParent.kt"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    platforms = ["CLOSURE"],
    deps = [":MethodOrderingTestParent"],
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
    platforms = [
        "CLOSURE",
    ],
)

kotlin_and_j2cl_library(
    name = "SimplePassingLib",
    srcs = ["SimplePassingTest.kt"],
)

j2cl_test_integration_test_data(
    name = "SimpleSuite",
    platforms = [
        "CLOSURE",
    ],
    deps = [":SimplePassingLib"],
)

j2cl_test_integration_test_data(
    name = "SuiteOfSuite",
    platforms = [
        "CLOSURE",
    ],
    deps = [":SimpleSuite-lib"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInAfterTest",
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeTest",
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsOnConstructionTest",
    platforms = [
        "CLOSURE",
    ],
)
