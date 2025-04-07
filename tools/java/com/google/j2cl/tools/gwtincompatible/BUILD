# Description:
#   This builds the gwtincompatible stripping tool.  This tool looks for @GwtIncompatible
#   annotations on source code elements and comments them out so that they do not get parsed by
#   J2CL.

load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("//transpiler/java/com/google/j2cl/common/bazel:jvm_flags.bzl", "JVM_FLAGS")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "gwtincompatible_lib",
    srcs = glob(
        ["*.java"],
        exclude = ["BazelGwtIncompatibleStripper.java"],
    ),
    visibility = ["//tools/javatests/com/google/j2cl/tools/gwtincompatible:__pkg__"],
    deps = [
        "//third_party:args4j",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)

java_binary(
    name = "GwtIncompatibleStripper",
    main_class = "com.google.j2cl.tools.gwtincompatible.GwtIncompatibleStripperCommandLineRunner",
    use_launcher = False,
    runtime_deps = [":gwtincompatible_lib"],
)

java_library(
    name = "gwtincompatible_worker_lib",
    srcs = ["BazelGwtIncompatibleStripper.java"],
    deps = [
        ":gwtincompatible_lib",
        "//third_party:args4j",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
    ],
)

java_binary(
    name = "GwtIncompatibleStripper_worker",
    jvm_flags = JVM_FLAGS,
    main_class = "com.google.j2cl.tools.gwtincompatible.BazelGwtIncompatibleStripper",
    use_launcher = False,
    visibility = ["//build_defs:toolchain_users"],
    runtime_deps = [":gwtincompatible_worker_lib"],
)
