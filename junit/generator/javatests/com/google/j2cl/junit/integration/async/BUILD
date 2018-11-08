licenses(["notice"])  # Apache 2.0

load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

# For debugging purposes it can be helpful to have the test log results of
# the integration tests to stdout. This can be done with:
# blaze test --test_arg=--output_command_line third_party/java_src/j2cl/junit/generator/javatests/com/google/j2cl/junit/integration/async:J2clAsyncTest
j2cl_test_integration_test(
    name = "J2clAsyncTest",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestAfterWithFailingAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestChainingWithException",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestNonAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestResolvesAfterDelay",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnsNullForPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWillTimeOut",
    ],
    test_data_java_only = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterNotJsType",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestTimeOutNotProvided",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWithLifeCycleMethodBeingAsync",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWithExpectedException",
    ],
    deps = ["//junit/generator/java/com/google/j2cl/junit/async"],
)
