# Sample for J2WASM.
#
# Note that J2WASM is experimental and it is NOT production ready.
#
# It is mostly developed to help with the evolution of Wasm GC specification and
# what is released externally here is minimal working version to provide .wat files
# for community experimentation and it doesn't reflect final workings of the product
# nor the Wasm spec.

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load(
    "@com_google_j2cl//build_defs:rules.bzl",
    "j2cl_application",
    "j2wasm_application",
    "j2wasm_library",
)

package(
    default_applicable_licenses = ["@com_google_j2cl//:j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
)

# This is the bazel target that compiles our J2WASM library.
# Since J2WASM currently does global compilation this only serves as a way
# to collect sources and byte code required for j2wasm_application compilation.
j2wasm_library(
    name = "helloworld",
    srcs = glob(["*.java"]),
)

# This is the bazel target that compiles and optimizes whole J2WASM app.
# It provides couple of convenient targets:
#  :app produces the app.wat and app.wasm that could be used for production.
#  :app_dev produces app_dev.wat and app_dev.wasm as development version
#
# e.g.:
#   $ bazel build samples/wasm/src/main/java/com/google/j2cl/samples/wasm:app
#
# Note that for .wat files (wasm binary output) to be available, you would
# need to have a recent version of binaryen available in your path.
j2wasm_application(
    name = "app",
    entry_points = ["com.google.j2cl.samples.wasm.HelloWorld#getHelloWorld"],
    deps = [":helloworld"],
)

# Below is an example of the JS wiring of the wasm app

closure_js_library(
    name = "hellojs",
    srcs = glob(["*.js"]),
    lenient = True,
    deps = [":app"],
)

# This is the bazel target that serves your J2WASM app.
#
# Give it a try:
#   $ bazel run samples/wasm/src/main/java/com/google/j2cl/samples/wasm:jsapp_dev_server
#
j2cl_application(
    name = "jsapp",
    entry_points = ["entry"],
    extra_dev_resources = [
        ":app_dev.wasm",  # Development output that is not optimized.
    ],
    deps = [":hellojs"],
)
