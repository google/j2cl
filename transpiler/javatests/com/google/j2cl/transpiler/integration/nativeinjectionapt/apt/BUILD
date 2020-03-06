load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

java_plugin(
    name = "apt",
    srcs = glob(["*.java"]),
    processor_class = "com.google.j2cl.transpiler.integration.nativeinjectionapt.apt.AptThatWritesNativeJsFile",
    deps = [
        "//third_party:auto_common",
        "//third_party:auto_service",
        "//third_party:auto_value",
        "//third_party:guava",
    ],
)

j2cl_library(
    name = "annotation",
    srcs = ["RunApt.java"],
    exported_plugins = [":apt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt:__pkg__"],
)
