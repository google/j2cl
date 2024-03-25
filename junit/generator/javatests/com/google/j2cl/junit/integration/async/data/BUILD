load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//junit/generator/javatests/com/google/j2cl/junit/apt:__pkg__",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async:__pkg__",
    ],
    # Tests in this directory are not tests that are meant to be run directly.
    # They serve as integration tests for J2clTestRunner.
    licenses = ["notice"],
)

ASYNC_TEST_DEPS = [
    "//junit/generator/javatests/com/google/j2cl/junit/integration/testing/async:timer",
    "//junit/generator/javatests/com/google/j2cl/junit/integration/testing/async:thenable",
    "//junit/generator/java/com/google/j2cl/junit/async:async",
]

j2cl_test_integration_test_data(
    name = "TestAfterWithFailingAsyncTest",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestAsyncAfterWillTimeOut",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestAsyncBeforeWillTimeOut",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestChainingWithException",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestNonAsyncTest",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestResolvesAfterDelay",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestMethodOrder",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestStructuralThenable",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestListenableFuture",
    deps = ASYNC_TEST_DEPS + ["//third_party:guava"],
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForAsyncAfter",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForAsyncBefore",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForPromise",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsVoidTimeoutProvided",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromise",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenParameterCount",
    deps = ASYNC_TEST_DEPS + ["//third_party:jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenParameterNotJsType",
    deps = ASYNC_TEST_DEPS + ["//third_party:jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenNameRedefined",
    deps = ASYNC_TEST_DEPS + ["//third_party:jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestTimeOutNotProvided",
    deps = ASYNC_TEST_DEPS + ["//third_party:guava"],
)

j2cl_test_integration_test_data(
    name = "TestWillTimeOut",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestWithExpectedException",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestFailingAsyncAfter",
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestFailingAsyncBefore",
    deps = ASYNC_TEST_DEPS,
)
