load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest:__pkg__"],
    licenses = ["notice"],
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
    name = "SingleParameterInitializedByConstructor",
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

j2cl_test_integration_test_data(
    name = "BeforeAndAfterParamTest",
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterParamWithFailingTest",
)

j2cl_test_integration_test_data(
    name = "IncorrectInjectedFieldCount",
)

j2cl_test_integration_test_data(
    name = "ConstructorWithIncorrectParameterCount",
)

j2cl_test_integration_test_data(
    name = "ThrowsInFloatToIntegerTest",
)

j2cl_test_integration_test_data(
    name = "IntegerToIntTest",
)

j2cl_test_integration_test_data(
    name = "AssumptionBeforeParamTest",
)
