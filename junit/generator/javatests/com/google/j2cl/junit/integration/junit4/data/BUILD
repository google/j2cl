load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

package(
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit4:__pkg__"],
    licenses = ["notice"],
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    enable_wasm = True,
    deps = [":BeforeAndAfterTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ChildWithNewMethodsTest",
    enable_wasm = True,
    deps = [":ChildTest-lib"],
)

j2cl_test_integration_test_data(
    name = "GrandChildWithNewMethodsTest",
    enable_wasm = True,
    deps = [":ChildWithNewMethodsTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ExpectedExceptionTest",
    enable_wasm = True,
)

java_and_j2cl_library(
    name = "IgnoreTestParent",
    srcs = ["IgnoreTestParent.java"],
)

j2cl_test_integration_test_data(
    name = "IgnoreTest",
    enable_wasm = True,
    deps = [":IgnoreTestParent"],
)

java_and_j2cl_library(
    name = "MethodOrderingTestParent",
    srcs = ["MethodOrderingTestParent.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    enable_wasm = True,
    deps = [":MethodOrderingTestParent"],
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
    enable_wasm = True,
)

java_and_j2cl_library(
    name = "SimplePassingLib",
    srcs = ["SimplePassingTest.java"],
)

j2cl_test_integration_test_data(
    name = "SimpleSuite",
    enable_wasm = True,
    deps = [":SimplePassingLib"],
)

j2cl_test_integration_test_data(
    name = "SuiteOfSuite",
    enable_wasm = True,
    deps = [":SimpleSuite-lib"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInAfterTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeTest",
    enable_wasm = True,
)

j2cl_test_integration_test_data(
    name = "ThrowsOnConstructionTest",
    enable_wasm = True,
)
