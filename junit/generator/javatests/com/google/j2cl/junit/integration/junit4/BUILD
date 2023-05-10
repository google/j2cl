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
    name = "JUnit4IntegrationTest1",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    tags = ["not_run:arm"],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:BeforeAndAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ChildTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ChildWithNewMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ExpectedExceptionTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:GrandChildWithNewMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:IgnoreTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:MethodOrderingTest",
    ],
)

j2cl_test_integration_test(
    name = "JUnit4IntegrationTest2",
    platforms = [
        "CLOSURE",
        "WASM",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimpleFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimplePassingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimpleSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SuiteOfSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInBeforeClassTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInBeforeTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsOnConstructionTest",
    ],
)

j2cl_test_integration_test(
    name = "J2ktJUnit4IntegrationTest",
    platforms = ["J2KT"],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:BeforeAndAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ExpectedExceptionTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:IgnoreTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimpleFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimplePassingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SimpleSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:SuiteOfSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInBeforeClassTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsInBeforeTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/junit4/data:ThrowsOnConstructionTest",
    ],
)
