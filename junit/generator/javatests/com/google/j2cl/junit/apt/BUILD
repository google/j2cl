package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "junit_processor_library",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "//junit/generator/java/com/google/j2cl/junit/apt:junit_processor",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromise-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenNameRedefined-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterCount-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnTypeNotStructuralPromiseThenParameterNotJsType-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestReturnsVoidTimeoutProvided-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestTimeOutNotProvided-lib",
        "//junit/generator/javatests/com/google/j2cl/junit/integration/async/data:TestWithExpectedException-lib",
        "//third_party:auto_common",
        "//third_party:auto_service",
        "//third_party:compile_testing",
        "//third_party:guava",
        "//third_party:gwt-jsinterop-annotations",
        "//third_party:jsr305_annotations",
        "//third_party:junit",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)

java_test(
    name = "J2clTestingProcessingStepTest",
    runtime_deps = [":junit_processor_library"],
)

java_test(
    name = "MethodSorterTest",
    srcs = ["MethodSorterTest.java"],
    deps = [
        "//junit/generator/java/com/google/j2cl/junit/apt:junit_processor",
        "//third_party:compile_testing",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//third_party:junit",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)
