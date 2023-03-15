load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["@com_google_j2cl//:j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
)

# This is the bazel target that compiles our J2CL library. Note that our HelloWorld library
# include both Java and JavaScript sources. Usually you don't need to have any JavaScript
# code in your library but it can come in handy in some situations.
# Globing beyond a package is considered a bad practice and for legacy apps. Keeping libraries
# not too big will help with parallelization and performance of the build.
j2cl_library(
    name = "helloworldlib",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    deps = [
        "@com_google_j2cl//:jsinterop-annotations-j2cl",
    ],
)
