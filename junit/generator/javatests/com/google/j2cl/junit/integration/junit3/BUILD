load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# For debugging purposes it can be helpful to have the test log results of
# the integration tests to stdout. This can be done with:
# blaze test --test_arg=--output_command_line third_party/java_src/j2cl/junit/generator/javatests/com/google/j2cl/junit/integration/junit3:JUnit3IntegrationTest
j2cl_test_integration_test(
    name = "JUnit3IntegrationTest",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:MethodOrderingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:ParentMethodTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:SetupAndTearDownTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:SimpleFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:SimplePassingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:StaticMethodTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:ThrowsInConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:ThrowsInSetupTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit3/data:ThrowsInTearDownTest",
    ],
)
