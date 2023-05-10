load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test_data",
    "kotlin_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration/kotlintest:__pkg__"],
    licenses = ["notice"],
)

j2cl_test_integration_test_data(
    name = "BeforeAndAfterTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ChildTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":BeforeAndAfterTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ChildWithNewMethodsTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":ChildTest-lib"],
)

j2cl_test_integration_test_data(
    name = "GrandChildWithNewMethodsTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":ChildWithNewMethodsTest-lib"],
)

j2cl_test_integration_test_data(
    name = "ExpectedExceptionTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

kotlin_and_j2cl_library(
    name = "IgnoreTestParent",
    srcs = ["IgnoreTestParent.kt"],
)

j2cl_test_integration_test_data(
    name = "IgnoreTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":IgnoreTestParent"],
)

kotlin_and_j2cl_library(
    name = "MethodOrderingTestParent",
    srcs = ["MethodOrderingTestParent.kt"],
)

j2cl_test_integration_test_data(
    name = "MethodOrderingTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = ["CLOSURE"],
    deps = [":MethodOrderingTestParent"],
)

j2cl_test_integration_test_data(
    name = "SimpleFailingTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "SimplePassingTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

kotlin_and_j2cl_library(
    name = "SimplePassingLib",
    srcs = ["SimplePassingTest.kt"],
)

j2cl_test_integration_test_data(
    name = "SimpleSuite",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":SimplePassingLib"],
)

j2cl_test_integration_test_data(
    name = "SuiteOfSuite",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
    deps = [":SimpleSuite-lib"],
)

j2cl_test_integration_test_data(
    name = "ThrowsInAfterTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsInBeforeTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)

j2cl_test_integration_test_data(
    name = "ThrowsOnConstructionTest",
    # TODO(b/225411185): Enable when RTA works with Kotlin output.
    enable_rta = False,
    platforms = [
        "CLOSURE",
    ],
)
