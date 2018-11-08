package(default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit4:__pkg__"])

licenses(["notice"])  # Apache 2.0

load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterTest",
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    deps = [":BeforeAndAfterTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ChildWithNewMethodsTest",
    deps = [":ChildTest-lib"],
)

j2cl_test_integration_test_data(
    name = "GrandChildWithNewMethodsTest",
    deps = [":ChildWithNewMethodsTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ExpectedExceptionTest",
)

java_and_j2cl_library(
    name = "MethodOrderingTestParent",
    srcs = ["MethodOrderingTestParent.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    deps = [":MethodOrderingTestParent"],
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
)

java_and_j2cl_library(
    name = "SimplePassingLib",
    srcs = ["SimplePassingTest.java"],
)

j2cl_test_integration_test_data(
    name = "SimpleSuite",
    deps = [":SimplePassingLib"],
)

j2cl_test_integration_test_data(
    name = "SuiteOfSuite",
    deps = [":SimpleSuite-lib"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInAfterTest",
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeTest",
)

j2cl_test_integration_test_data(
    name = "ThrowsOnConstructionTest",
)
