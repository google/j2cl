# Description:
#   j2cl/jre: JRE subset for transpilation.

load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_binary")
load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test", "j2kt_native_library", "j2wasm_test")
load("//javascript/tools/jscompiler/builddefs:flags.bzl", "VERBOSE_WARNING_FLAGS_STRICT", "WHITESPACE_ONLY_FLAGS")
load("//third_party/bazel_rules/rules_closure/closure:defs.bzl", "closure_js_binary")
load("@rules_java//java:defs.bzl", "java_test")
load(":j2cl_multi_test.bzl", "j2cl_multi_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

JS_ONLY_SRCS = glob([
    "**/Js*.java",
    "**/*.js",
])

SRCS = glob(
    ["**/*.java"],
    exclude = JS_ONLY_SRCS,
)

j2cl_library(
    name = "emul_tests_lib",
    testonly = 1,
    srcs = SRCS,
    # Safe here as this is for tests only and there are no downstream users.
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    javacopts = [
        "-Xep:DoNotCall:OFF",
        "-Xep:CheckReturnValue:OFF",
        "-Xep:EqualsIncompatibleType:OFF",
        "-Xep:ReturnValueIgnored:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

j2kt_native_library(
    name = "emul_tests_lib-j2kt-native",
    testonly = 1,
    srcs = glob(
        ["**/*.java"],
        exclude = JS_ONLY_SRCS + [
            "com/google/j2cl/jre/AllTests.java",
            "com/google/j2cl/jre/EmulJava9Suite.java",
            "com/google/j2cl/jre/java9/**/*.java",
        ],
    ),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-native",
        "//third_party:junit-j2kt-native",
    ],
)

# This runs all test for emul in JVM, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = SRCS,
    javacopts = [
        "-Xep:DoNotCall:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:EqualsIncompatibleType:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
        "-Xep:ReturnValueIgnored:OFF",
    ],
    test_class = "com.google.j2cl.jre.AllTests",
    deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:jsinterop-annotations",
        "//third_party:junit",
    ],
)

j2cl_multi_test(
    name = "BigDecimal",
    shard_count = 2,
    test_class = "com.google.j2cl.jre.BigDecimalSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "BigInteger",
    shard_count = 2,
    test_class = "com.google.j2cl.jre.BigIntegerSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "Collections",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.CollectionsSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "Concurrent",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.ConcurrentSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "Emul",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.EmulSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "EmulJava8",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.EmulJava8Suite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "EmulJava9",
    enable_kt_native = False,
    shard_count = 2,
    test_class = "com.google.j2cl.jre.EmulJava9Suite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "Lang",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.LangSuite",
    deps = [":emul_tests_lib"],
)

j2cl_multi_test(
    name = "DateTimeSuite",
    test_class = "com.google.j2cl.jre.DateTimeSuite",
    deps = [":emul_tests_lib"],
)

j2cl_library(
    name = "js_tests_lib",
    testonly = 1,
    srcs = JS_ONLY_SRCS,
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

j2cl_multi_test(
    name = "JsSuite",
    enable_kt_native = False,
    enable_wasm = False,
    test_class = "com.google.j2cl.jre.JsSuite",
    deps = [":js_tests_lib"],
)

# A smoke check that verifies es5 transpiled test works
j2cl_test(
    name = "ConcurrentSuite_es5",
    browsers = ["//testing/web/browsers:chrome-linux"],
    generate_build_test = False,
    jvm_flags = ["-Djsrunner.net.transpileMode=always"],
    shard_count = 4,
    test_class = "com.google.j2cl.jre.ConcurrentSuite",
    runtime_deps = [
        ":emul_tests_lib",
        "//third_party/java/junit:junit-j2cl",
    ],
)

j2wasm_test(
    name = "ConcurrentSuite_wasm_magic_string_imports",
    shard_count = 4,
    test_class = "com.google.j2cl.jre.ConcurrentSuite",
    use_magic_string_imports = True,
    runtime_deps = [
        ":emul_tests_lib-j2wasm",
        "//third_party/java/junit:junit-j2wasm",
    ],
)

build_test(
    name = "conformance_test",
    tags = ["j2cl"],
    targets = [
        ":conformance_binary",
        ":conformance_binary_head",
        ":conformance_binary_whitespace_only",
    ],
)

COMFORMANCE_CHECK_FLAGS = VERBOSE_WARNING_FLAGS_STRICT + [
    "--conformance_config=conformance_proto.txt",
    "--jscomp_error=conformanceViolations",
    "--jscomp_error=unusedLocalVariables",
    # TODO(michaelthomas): Enable once all errors are fixed. Yes, seriously.
    # "--jscomp_error=reportUnknownTypes",
]

closure_js_binary(
    name = "conformance_binary",
    defs = COMFORMANCE_CHECK_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    use_precompiled_libraries = False,
    deps = ["//:jre"],
)

closure_js_binary(
    name = "conformance_binary_head",
    compiler = "//javascript/tools/jscompiler:head",
    defs = COMFORMANCE_CHECK_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    use_precompiled_libraries = False,
    deps = ["//:jre"],
)

closure_js_binary(
    name = "conformance_binary_whitespace_only",
    compiler = "//javascript/tools/jscompiler:head",
    defs = COMFORMANCE_CHECK_FLAGS + WHITESPACE_ONLY_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    use_precompiled_libraries = False,
    deps = ["//:jre"],
)

closure_js_binary(
    name = "conformance_binary_opensource",
    dependency_mode = "SORT_ONLY",
    suppress_on_all_sources_in_transitive_closure = [
        "analyzerChecks",
        "deprecated",
    ],
    deps = ["//:jre"],
)

genrule(
    name = "file_empty_check",
    outs = ["file_empty_check.sh"],
    cmd = "echo '[ ! -s $$1 ]' > $@",
)

sh_test(
    name = "opensource_output_empty_test",
    srcs = ["file_empty_check.sh"],
    args = ["$(location conformance_binary_opensource-stderr.txt)"],
    data = ["conformance_binary_opensource-stderr.txt"],
    tags = ["j2cl"],
)
