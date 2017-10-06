# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    testonly = 1,
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
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/junit:junit-j2cl",
    ],
)

j2cl_source_copy(
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
        "//third_party/java/gwt:gwt-javaemul-internal-annotations",
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

load(":j2cl_multi_test.bzl", "j2cl_multi_test")

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
    # Bootstrap files are dropped in compiled mode so we add it as a _js_srcs here.
    # TODO(b/62039178): Remove this.
    _js_srcs = [":test_defines"],
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

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "conformance_test",
    targets = [
        ":conformance_binary",
        ":conformance_binary_head",
    ],
)

load("//build_def:j2cl_util.bzl", "J2CL_OPTIMIZED_DEFS")
load("//javascript/tools/jscompiler/builddefs:flags.bzl", "VERBOSE_WARNING_FLAGS_STRICT")

COMFORMANCE_CHECK_FLAGS = J2CL_OPTIMIZED_DEFS + VERBOSE_WARNING_FLAGS_STRICT + [
    "--conformance_config=conformance_proto.txt",
    "--jscomp_error=conformanceViolations",
    # TODO(michaelthomas): Enable once all errors are fixed. Yes, seriously.
    # "--jscomp_error=reportUnknownTypes",
    # Fail-fast on invalid trailing commas for older browsers
    "--language_in=ECMASCRIPT_2015",
]

js_binary(
    name = "conformance_binary",
    compile = 1,
    defs = COMFORMANCE_CHECK_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    deps = ["//jre/java:jre"],
)

js_binary(
    name = "conformance_binary_head",
    compile = 1,
    compiler = "//javascript/tools/jscompiler:head",
    defs = COMFORMANCE_CHECK_FLAGS,
    extra_inputs = ["conformance_proto.txt"],
    deps = ["//jre/java:jre"],
)

# TODO(dankuka): Eventually we want to provide a library target for apps that
# need to be compatible with both gwt tests and j2cl tests, but this will
# require some more careful thought.
java_library(
    name = "gwt_test_case",
    testonly = 1,
    srcs = ["com/google/gwt/junit/client/GWTTestCase.java"],
    deps = ["//third_party/java/junit"],
)
