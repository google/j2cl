package(default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/junit3:__pkg__"])

licenses(["notice"])  # Apache 2.0

load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "java_and_j2cl_library",
)

java_and_j2cl_library(
    name = "MethodOrderingParentTest",
    srcs = ["MethodOrderingParentTest.java"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    deps = [":MethodOrderingParentTest"],
)

java_and_j2cl_library(
    name = "parent_class",
    srcs = ["ParentMethodParentClass.java"],
)

j2cl_test_integration_test_data(
    name = "ParentMethodTest",
    deps = [
        ":parent_class",
    ],
)

j2cl_test_integration_test_data(
    name = "SetupAndTearDownTest",
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
)

j2cl_test_integration_test_data(
    name = "StaticMethodTest",
)

j2cl_test_integration_test_data(
    name = "ThrowsInConstructorTest",
)

j2cl_test_integration_test_data(
    name = "ThrowsInSetupTest",
)

j2cl_test_integration_test_data(
    name = "ThrowsInTearDownTest",
)
