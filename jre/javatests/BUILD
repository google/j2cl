# Description:
#   j2cl/jre: JRE subset for transpilation.

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_binary")
load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")
load("//jre/java:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt", "java_source_copy")
load(":j2cl_multi_test.bzl", "j2cl_multi_test")
load("@bazel_tools//tools/build_rules:build_test.bzl", "build_test")
load("//build_defs:rules.bzl", "j2cl_library")
load("//javascript/tools/jscompiler/builddefs:flags.bzl", "VERBOSE_WARNING_FLAGS_STRICT", "WHITESPACE_ONLY_FLAGS")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    testonly = 1,
    # Bootstrap files are dropped in compiled mode so we add it as a _js_srcs here.
    # TODO(b/62039178): Remove this.
    extra_js_srcs = [":test_defines"],
    generate_build_test = False,
    javacopts = [
        "-Xep:CheckReturnValue:OFF",
        "-Xep:EqualsIncompatibleType:OFF",
        "-Xep:ReturnValueIgnored:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
    ],
    mirrored_files = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

java_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = [
        # Emulated charset contains classes extending from
        # java.nio.charset.Charset that are not implementing all Java methods.
        # This is okay in the emulated version since emulated
        # java.nio.charset.Charset does not have these methods.
        "javaemul/internal/EmulatedCharset.java",
    ],
)

# This runs all GWTTestCases for emul in a pure JRE, this way we can make sure that our tests
# assert the right behaviour for the JRE emulation.
java_test(
    name = "AllEmulTest_Java",
    size = "small",
    srcs = [
        ":emul_tests_lib_java_files",
        ":java_emul_internal",
    ],
    javacopts = [
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:EqualsIncompatibleType:OFF",
        "-Xep:SelfComparison:OFF",
        "-Xep:SelfEquality:OFF",
        "-Xep:SelfEquals:OFF",
        "-Xep:ReturnValueIgnored:OFF",
    ],
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
        "//third_party:junit",
    ],
)

j2cl_multi_test(
    name = "BigDecimal",
    shard_count = 5,
    test_class = "com.google.gwt.emultest.BigDecimalSuite",
)

j2cl_multi_test(
    name = "BigInteger",
    shard_count = 8,
    test_class = "com.google.gwt.emultest.BigIntegerSuite",
)

j2cl_multi_test(
    name = "Collections",
    shard_count = 16,
    test_class = "com.google.gwt.emultest.CollectionsSuite",
)

j2cl_multi_test(
    name = "Concurrent",
    shard_count = 5,
    test_class = "com.google.gwt.emultest.ConcurrentSuite",
)

j2cl_multi_test(
    name = "Emul",
    bootstrap_files = [":test_defines"],
    shard_count = 16,
    test_class = "com.google.gwt.emultest.EmulSuite",
)

genrule(
    name = "test_defines",
    outs = ["test_defines.js"],
    cmd = "echo \"/** @define {string} */ var someConfigurationProperty='conf';\" > $@",
)

j2cl_multi_test(
    name = "EmulJava8",
    shard_count = 8,
    test_class = "com.google.gwt.emultest.EmulJava8Suite",
)

# A smoke check that verifies es5 transpiled test works
j2cl_test(
    name = "ConcurrentSuite_es5",
    browsers = ["//testing/web/browsers:ie11-win7"],
    generate_build_test = False,
    jvm_flags = ["-Djsrunner.net.transpileMode=always"],
    shard_count = 5,
    test_class = "com.google.gwt.emultest.ConcurrentSuite",
    runtime_deps = [
        ":emul_tests_lib",
        "//third_party/java/junit:junit-j2cl",
    ],
)

build_test(
    name = "conformance_test",
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
    deps = ["//jre/java:jre"],
)

closure_js_binary(
    name = "conformance_binary_head",
    compiler = "//javascript/tools/jscompiler:head",
    defs = COMFORMANCE_CHECK_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    deps = ["//jre/java:jre"],
)

closure_js_binary(
    name = "conformance_binary_whitespace_only",
    compiler = "//javascript/tools/jscompiler:head",
    defs = COMFORMANCE_CHECK_FLAGS + WHITESPACE_ONLY_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    deps = ["//jre/java:jre"],
)

closure_js_binary(
    name = "conformance_binary_opensource",
    dependency_mode = "SORT_ONLY",
    suppress_on_all_sources_in_transitive_closure = [
        "analyzerChecks",
        "deprecated",
    ],
    deps = ["//jre/java:jre"],
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
)

# TODO(dankuka): Eventually we want to provide a library target for apps that
# need to be compatible with both gwt tests and j2cl tests, but this will
# require some more careful thought.
j2cl_library(
    name = "gwt_test_case-j2cl",
    testonly = 1,
    srcs = ["com/google/gwt/junit/client/GWTTestCase.java"],
    visibility = [
        "//javatests/com/google/protobuf/contrib/gwt:__subpackages__",
    ],
    deps = ["//third_party:junit-j2cl"],
)
