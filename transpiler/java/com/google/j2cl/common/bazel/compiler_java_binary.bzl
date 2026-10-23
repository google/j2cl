load("@rules_java//java:java_binary.bzl", "java_binary")
load(":jvm_flags.bzl", "JVM_FLAGS")

visibility(["//..."])

def compiler_java_binary(name, runtime_deps = [], extra_jvm_flags = [], **kwargs):
    java_binary(
        name = name,
        jvm_flags = JVM_FLAGS + extra_jvm_flags,
        runtime_deps = runtime_deps,
        visibility = ["//build_defs:toolchain_users"],
        **kwargs
    )
