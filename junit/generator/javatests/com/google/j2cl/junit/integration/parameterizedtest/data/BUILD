load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
)

package(
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest:__pkg__"],
    licenses = ["notice"],  # Apache 2.0
)

j2cl_test_integration_test_data(
    name = "SimpleParameterizedTest",
)

j2cl_test_integration_test_data(
    name = "ParameterizedTestInitializedByConstructor",
)

j2cl_test_integration_test_data(
    name = "SingleArrayParameterTest",
)

j2cl_test_integration_test_data(
    name = "SingleIterableParameterTest",
)

j2cl_test_integration_test_data(
    name = "SingleIterableParameterProvidedByArrayTest",
)

j2cl_test_integration_test_data(
    name = "SingleParameterOfArrayTypeTest",
)

j2cl_test_integration_test_data(
    name = "MultipleIterableParametersTest",
)

j2cl_test_integration_test_data(
    name = "MultipleDataMethodsTest",
)

j2cl_test_integration_test_data(
    name = "ParentTest",
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    deps = [":ParentTest-lib"],
)
