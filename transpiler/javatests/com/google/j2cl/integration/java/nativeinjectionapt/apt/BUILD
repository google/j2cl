load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_plugin(
    name = "apt",
    srcs = glob(["*.java"]),
    processor_class = "nativeinjectionapt.apt.AptThatWritesNativeJsFile",
    deps = [
        "//third_party:auto_common",
        "//third_party:auto_service",
        "//third_party:auto_value",
        "//third_party:guava",
    ],
)

j2cl_library(
    name = "annotation-j2cl",
    srcs = ["RunApt.java"],
    exported_plugins = [":apt"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/nativeinjectionapt:__pkg__"],
)
