load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_test_integration_test(
    name = "KotlinIntegrationTest",
    platforms = ["CLOSURE"],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:BeforeAndAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ChildTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ChildWithNewMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ExpectedExceptionTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:GrandChildWithNewMethodsTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:IgnoreTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:MethodOrderingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:SimpleFailingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:SimplePassingTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:SimpleSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:SuiteOfSuite",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ThrowsInAfterTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ThrowsInBeforeTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest/data:ThrowsOnConstructionTest",
    ],
)
