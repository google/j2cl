load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "j2cl_test_integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
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
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:JsExceptionNonJsConstructorStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:JsExceptionStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:LambdaStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:RecursiveStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:SimpleThrowingStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInBridgeMethod",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInClassInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInInstanceInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsConstructorTest",
    ],
)

j2cl_test_integration_test(
    name = "StacktraceIntegration2Test",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:DuplicateFileNameStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ExceptionWithCauseStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:NativeStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsFunction",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInJsProperty",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:ThrowsInNativeJs",
    ],
)

j2cl_test_integration_test(
    name = "WasmStacktraceIntegrationTest",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    platforms = ["WASM"],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:SimpleThrowingStacktraceTest",
    ],
)

j2cl_test_integration_test(
    name = "KotlinStacktraceIntegration1Test",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinAnonymousClassesStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinCustomExceptionStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinFillInStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinJsExceptionNonJsConstructorStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinJsExceptionStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinLambdaStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinRecursiveStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInBridgeMethod",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInClassInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInInstanceInitializer",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInJsConstructorTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:SimpleKotlinThrowingStacktraceTest",
    ],
)

j2cl_test_integration_test(
    name = "KotlinStacktraceIntegration2Test",
    extra_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:stacktrace-files",
    ],
    test_data = [
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinDuplicateFileNameStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinExceptionWithCauseStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinNativeStacktraceTest",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInJsFunction",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInJsProperty",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/stacktrace/data:KotlinThrowsInNativeJs",
    ],
)
