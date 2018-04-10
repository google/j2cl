# Apache2
licenses(["notice"])

java_test(
    name = "JavaPreprocessorTest",
    srcs = ["JavaPreprocessorTest.java"],
    deps = [
        "//java/com/google/common/base",
        "//third_party/java/junit",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
    ],
)

java_test(
    name = "UnusedImportsNodeCollectorTest",
    srcs = ["UnusedImportsNodeCollectorTest.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/eclipse:eclipse-jdt-core-4.7",
        "//third_party/java/junit",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
    ],
)

java_test(
    name = "StripperTest",
    srcs = ["StripperTest.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/io",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
    ],
)
