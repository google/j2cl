# Apache2
licenses(["notice"])

java_plugin(
    name = "apt",
    srcs = glob(["*.java"]),
    processor_class = "com.google.j2cl.transpiler.integration.nativeinjectionapt.apt.AptThatWritesNativeJsFile",
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/auto:auto_service",
        "//third_party/java/auto:auto_value",
        "//third_party/java/auto:common",
        "//third_party/java/jsr305_annotations",
    ],
)

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "annotation",
    srcs = ["RunApt.java"],
    exported_plugins = [":apt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt:__pkg__"],
)
