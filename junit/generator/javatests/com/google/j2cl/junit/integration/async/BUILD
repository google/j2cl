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
# blaze test --test_arg=--output_command_line junit/generator/javatests/com/google/j2cl/junit/integration/async:J2clAsyncTest
j2cl_test_integration_test(
    name = "J2clAsyncTest",
    tags = ["not_run:arm"],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestAfterWithFailingAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestAsyncAfterWillTimeOut",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestAsyncBeforeWillTimeOut",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestChainingWithException",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestFailingAsyncAfter",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestFailingAsyncBefore",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestMethodOrder",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestNonAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestResolvesAfterDelay",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnsNullForAsyncAfter",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnsNullForAsyncBefore",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnsNullForPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestStructuralThenable",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWillTimeOut",
    ],
    test_data_java_only = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestListenableFuture",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterNotJsType",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestTimeOutNotProvided",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWithExpectedException",
    ],
    deps = ["//junit/generator/java/com/google/j2cl/junit/async"],
)
