load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit3:__pkg__"],
    licenses = ["notice"],
)

java_and_j2cl_library(
    name = "MethodOrderingParentTest",
    srcs = ["MethodOrderingParentTest.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    deps = [":MethodOrderingParentTest"],
)

java_and_j2cl_library(
    name = "parent_class",
    srcs = ["ParentMethodParentClass.java"],
)

j2cl_test_integration_test_data(
    name = "ParentMethodTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
    deps = [
        ":parent_class",
    ],
)

j2cl_test_integration_test_data(
    name = "SetupAndTearDownTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
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

j2cl_test_integration_test_data(
    name = "StaticMethodTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInConstructorTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInSetupTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInTearDownTest",
    platforms = [
        "CLOSURE",
        "WASM",
        "J2KT",
    ],
)
