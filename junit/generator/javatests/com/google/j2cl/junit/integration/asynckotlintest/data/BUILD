load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/asynckotlintest:__pkg__"],
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
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestAsyncAfterWillTimeOut",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestAsyncBeforeWillTimeOut",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestChainingWithException",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestNonAsyncTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestResolvesAfterDelay",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestMethodOrder",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestStructuralThenable",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestListenableFuture",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS + ["//third_party:guava"],
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForAsyncAfter",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForAsyncBefore",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsNullForPromise",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnsVoidTimeoutProvided",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromise",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenParameterCount",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS + ["//third_party:gwt-jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenParameterNotJsType",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS + ["//third_party:gwt-jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestReturnTypeNotStructuralPromiseThenNameRedefined",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS + ["//third_party:gwt-jsinterop-annotations"],
)

j2cl_test_integration_test_data(
    name = "TestTimeOutNotProvided",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS + ["//third_party:guava"],
)

j2cl_test_integration_test_data(
    name = "TestWillTimeOut",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestWithExpectedException",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestFailingAsyncAfter",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)

j2cl_test_integration_test_data(
    name = "TestFailingAsyncBefore",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    deps = ASYNC_TEST_DEPS,
)
