load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit4:__pkg__"],
    licenses = ["notice"],
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":BeforeAndAfterTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ChildWithNewMethodsTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":ChildTest-lib"],
)

j2cl_test_integration_test_data(
    name = "GrandChildWithNewMethodsTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":ChildWithNewMethodsTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ExpectedExceptionTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

java_and_j2cl_library(
    name = "IgnoreTestParent",
    srcs = ["IgnoreTestParent.java"],
)

j2cl_test_integration_test_data(
    name = "IgnoreTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
    deps = [":IgnoreTestParent"],
)

java_and_j2cl_library(
    name = "MethodOrderingTestParent",
    srcs = ["MethodOrderingTestParent.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":MethodOrderingTestParent"],
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

java_and_j2cl_library(
    name = "SimplePassingLib",
    srcs = ["SimplePassingTest.java"],
)

j2cl_test_integration_test_data(
    name = "SimpleSuite",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
    deps = [":SimplePassingLib"],
)

j2cl_test_integration_test_data(
    name = "SuiteOfSuite",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
    deps = [":SimpleSuite-lib"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInAfterTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeClassTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsOnConstructionTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)
