load("@rules_java//java:java_binary.bzl", "java_binary")
load("//transpiler/java/com/google/j2cl/common/bazel:jvm_flags.bzl", "JVM_FLAGS")

visibility("private")

def compiler_java_binary(name, runtime_deps = [], extra_jvm_flags = [], **kwargs):
    java_binary(
        name = name,
        jvm_flags = JVM_FLAGS + extra_jvm_flags + select({
            ":profiling_disabled": [],
            "//conditions:default": [
                "-XX:GoogleAgentFlags=-contentionz,-codez,-histogram,-heapz,-native_heapz," +
                "-perf_map,cpu_profile_stack_limit:256,cpu_samples:200",
            ],
        }),
        runtime_deps = runtime_deps + select({
            ":profiling_disabled": [],
            "//conditions:default": [
                "//transpiler/java/com/google/j2cl/common/bazel/profiler:profiler_impl",
            ],
        }),
        launcher = select({
            ":profiling_disabled": None,
            "//conditions:default": "//devtools/java/launcher:run_java",
        }),
        visibility = ["//build_defs:toolchain_users"],
        **kwargs
    )
