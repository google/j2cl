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
# blaze test --test_arg=--output_command_line junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest:KotlinAsyncTest
j2cl_test_integration_test(
    name = "KotlinAsyncTest",
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestAfterWithFailingAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestAsyncAfterWillTimeOut",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestAsyncBeforeWillTimeOut",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestChainingWithException",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestFailingAsyncAfter",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestFailingAsyncBefore",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestMethodOrder",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestNonAsyncTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestResolvesAfterDelay",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnsNullForAsyncAfter",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnsNullForAsyncBefore",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnsNullForPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestStructuralThenable",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestWillTimeOut",
    ],
    test_data_java_only = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestListenableFuture",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnTypeNotStructuralPromise",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnTypeNotStructuralPromiseThenParameterCount",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestReturnTypeNotStructuralPromiseThenParameterNotJsType",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestTimeOutNotProvided",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest/data:TestWithExpectedException",
    ],
    deps = ["//junit/generator/java/com/google/j2cl/junit/async"],
)
