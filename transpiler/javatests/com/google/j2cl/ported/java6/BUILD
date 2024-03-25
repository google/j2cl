load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "CompilerSuiteLib",
    testonly = 1,
    srcs = glob([
        "**/*.java",
        "**/*.js",
    ]),
    javacopts = [
        "-XepDisableAllChecks",
    ],
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
        "//third_party:truth-j2cl",
    ],
)

j2cl_test(
    name = "CompilerSuite_uncompiled",
    shard_count = 8,
    test_class = "com.google.j2cl.ported.java6.CompilerSuite",
    runtime_deps = [":CompilerSuiteLib"],
)

j2cl_test(
    name = "CompilerSuite_compiled",
    compile = 1,
    shard_count = 8,
    test_class = "com.google.j2cl.ported.java6.CompilerSuite",
    runtime_deps = [":CompilerSuiteLib"],
)
