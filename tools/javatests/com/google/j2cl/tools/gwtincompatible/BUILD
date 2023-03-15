package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "GwtIncompatibleStripperTest",
    srcs = ["GwtIncompatibleStripperTest.java"],
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
