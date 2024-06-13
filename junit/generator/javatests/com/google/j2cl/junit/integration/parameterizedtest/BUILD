load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# For debugging purposes it can be helpful to have the test log results of
# the integration tests to stdout. This can be done with:
# blaze test --test_arg=--output_command_line junit/generator/javatests/com/google/j2cl/junit/integration/junit4:JUnit4IntegrationTest
j2cl_test_integration_test(
    name = "J2clParameterizedIntegrationTest1",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:MultipleDataMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:MultipleIterableParametersTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ParameterizedTestInitializedByConstructor",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SimpleParameterizedTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleArrayParameterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleIterableParameterProvidedByArrayTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleIterableParameterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleParameterInitializedByConstructor",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleParameterOfArrayTypeTest",
    ],
)

j2cl_test_integration_test(
    name = "J2clParameterizedIntegrationTest2",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:AssumptionBeforeParamTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:BeforeAndAfterParamTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:BeforeAndAfterParamWithFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ChildTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ConstructorWithIncorrectParameterCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:IncorrectInjectedFieldCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:IntegerToIntTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ParentTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ThrowsInFloatToIntegerTest",
    ],
)
