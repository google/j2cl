load("@rules_java//java:defs.bzl", "java_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "gwtincompatibleMain",
    srcs = [
        "External.java",
        "Main.java",
    ],
    deps = [
        ":gwtincompatibleOther",
        "//third_party:guava",
    ],
)

java_library(
    name = "gwtincompatibleOther",
    srcs = ["Other.java"],
)

integration_test(
    name = "gwtincompatible-with-srcjar",
    srcs = [
        ":libgwtincompatibleMain-src.jar",
        ":libgwtincompatibleOther-src.jar",
    ],
    # @GwtIncompatible is not supported on the JVM.
    enable_jvm_test = False,
    enable_kt = False,
)
