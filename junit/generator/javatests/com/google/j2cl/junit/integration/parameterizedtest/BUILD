load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(licenses = ["notice"])

# For debugging purposes it can be helpful to have the test log results of
# the integration tests to stdout. This can be done with:
# blaze test --test_arg=--output_command_line junit/generator/javatests/com/google/j2cl/junit/integration/junit4:JUnit4IntegrationTest
j2cl_test_integration_test(
    name = "J2clParameterizedIntegrationTest",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SimpleParameterizedTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ParameterizedTestInitializedByConstructor",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:MultipleDataMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:MultipleIterableParametersTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleArrayParameterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleIterableParameterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleParameterInitializedByConstructor",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleIterableParameterProvidedByArrayTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:SingleParameterOfArrayTypeTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ParentTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ChildTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:BeforeAndAfterParamTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:BeforeAndAfterParamWithFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:ConstructorWithIncorrectParameterCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/parameterizedtest/data:IncorrectInjectedFieldCount",
    ],
)
