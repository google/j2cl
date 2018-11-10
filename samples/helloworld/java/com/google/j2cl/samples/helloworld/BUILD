load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_binary", "closure_js_library")

licenses(["notice"])  # Apache 2.0

load("//build_defs:rules.bzl", "J2CL_OPTIMIZED_DEFS")

closure_js_library(
    name = "app",
    srcs = ["app.js"],
    deps = ["//samples/helloworld/java/com/google/j2cl/samples/helloworldlib"],
)

# This is the bazel target that compiles and optimizes whole JavaScript / J2CL app.
# TODO(b/117998344): Discuss outputs and some of the optimizations
closure_js_binary(
    name = "helloworld",
    defs = J2CL_OPTIMIZED_DEFS + [
        "--rewrite_polyfills=false",
        "--entry_point=goog:j2cl.samples.app",
    ],
    deps = [":app"],
)
