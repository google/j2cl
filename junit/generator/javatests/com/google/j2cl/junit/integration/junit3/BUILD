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
# blaze test --test_arg=--output_command_line junit/generator/javatests/com/google/j2cl/junit/integration/junit3:JUnit3IntegrationTest
j2cl_test_integration_test(
    name = "JUnit3IntegrationTest",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    tags = ["not_run:arm"],
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

j2cl_test_integration_test(
    name = "J2ktJUnit3IntegrationTest",
    platforms = ["J2KT"],
    test_data = [
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
