load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Note that tests are manually sharded to avoid hitting 50 hard shard limit.

j2cl_test_integration_test(
    name = "StacktraceIntegration1Test",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:AnonymousClassesStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:CustomExceptionStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:FillInStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:JsExceptionStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:JsExceptionNonJsConstructorStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:LambdaStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:RecursiveStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:SimpleThrowingStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInInstanceInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInClassInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInBridgeMethod",
    ],
)

j2cl_test_integration_test(
    name = "StacktraceIntegration2Test",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:NativeStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInNativeJs",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsFunction",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsProperty",
    ],
)
