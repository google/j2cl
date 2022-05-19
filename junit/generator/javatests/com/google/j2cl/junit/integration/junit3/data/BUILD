load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

package(
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit3:__pkg__"],
    licenses = ["notice"],
)

java_and_j2cl_library(
    name = "MethodOrderingParentTest",
    srcs = ["MethodOrderingParentTest.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    enable_wasm = True,
    deps = [":MethodOrderingParentTest"],
)

java_and_j2cl_library(
    name = "parent_class",
    srcs = ["ParentMethodParentClass.java"],
)

j2cl_test_integration_test_data(
    name = "ParentMethodTest",
    enable_wasm = True,
    deps = [
        ":parent_class",
    ],
)

j2cl_test_integration_test_data(
    name = "SetupAndTearDownTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "StaticMethodTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ThrowsInConstructorTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ThrowsInSetupTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ThrowsInTearDownTest",
    enable_wasm = True,
)
