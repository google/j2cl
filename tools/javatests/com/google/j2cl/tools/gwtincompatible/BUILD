# Apache2
licenses(["notice"])

java_test(
    name = "JavaPreprocessorTest",
    srcs = ["JavaPreprocessorTest.java"],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
    ],
)

java_test(
    name = "UnusedImportsNodeCollectorTest",
    srcs = ["UnusedImportsNodeCollectorTest.java"],
    deps = [
        "//third_party:guava",
        "//third_party:jdt-core",
        "//third_party:junit",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
    ],
)

java_test(
    name = "StripperTest",
    srcs = ["StripperTest.java"],
    jvm_flags = [
        "-Dj2cl.deterministicTimestamps=true",
    ],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:truth",
        "//tools/java/com/google/j2cl/tools/gwtincompatible:gwtincompatible_lib",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
