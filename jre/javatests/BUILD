# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "emul_tests_lib",
    generate_build_test = False,
    mirrored_files = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_test"],
    deps = [
        "//jre/java:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

j2cl_source_copy(
    name = "java_emul_internal",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java_emul_internal"],
    excludes = [
        "javaemul/internal/EmulatedCharset.java",
        "javaemul/internal/HashCodes.java",  # relies on java8 only APIs
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
    test_class = "com.google.gwt.emultest.AllTests",
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/junit",
    ],
)

j2cl_source_copy(
    name = "emul_tests_java8_lib",
    srcs = ["//third_party/java_src/gwt/svn/trunk/user:java8_emul_test"],
)

# since our tests contain java8 code that we can not compile in g3 yet
# we use a genrule to compile them (like in jre/java/).
# TODO(dankurka): remove this once we have java8 compilation in g3.
java_binary(
    name = "jre_test_deps",
    testonly = 1,
    create_executable = 0,
    runtime_deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
        "//third_party/java/jdk:jdk8-google-v7-64/jre/lib/rt.jar",
        "//third_party/java/junit",
    ],
)

commands = [
    "set -e",
    "mkdir compile_out",
    "third_party/java/jdk/jdk8-google-v7-64/bin/javac -source 8 -target 8 -d compile_out" +
    " -cp $(location :jre_test_deps_deploy.jar)" +
    " $(SRCS)",
    "cd compile_out/",
    "zip -q -r tmp.jar *",
    "cd ..",
    "cp compile_out/tmp.jar $(OUTS)",
]

genrule(
    name = "compile_jre8_tests",
    testonly = 1,
    srcs = [
        ":emul_tests_java8_lib",
        ":emul_tests_lib_java_files",
        ":java_emul_internal",
    ],
    outs = ["jre8_tests.jar"],
    cmd = "\n".join(commands),
    tools = [
        ":jre_test_deps_deploy.jar",
        "//third_party/java/jdk:jdk8-google-v7-64",
    ],
)

java_import(
    name = "jre_tests",
    jars = ["jre8_tests.jar"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

java_test(
    name = "AllEmulTest_Java8",
    size = "small",
    test_class = "com.google.gwt.emultest.EmulJava8Suite",
    runtime_deps = [
        ":jre_tests",
        "//third_party/java/junit",
    ],
)

load(":j2cl_multi_test.bzl", "j2cl_multi_test")

j2cl_multi_test(
    name = "BigDecimal",
    test_class = "com.google.gwt.emultest.BigDecimalTests",
)

j2cl_multi_test(
    name = "BigInteger",
    test_class = "com.google.gwt.emultest.BigIntegerTests",
)

j2cl_multi_test(
    name = "Collections",
    test_class = "com.google.gwt.emultest.CollectionsTests",
)

j2cl_multi_test(
    name = "Emul",
    _js_srcs = ["test_defines.js"],
    bootstrap_files = ["test_defines.js"],
    test_class = "com.google.gwt.emultest.EmulTests",
)

genrule(
    name = "test_defines",
    outs = ["test_defines.js"],
    cmd = "echo \"/** @define {string} */ var someConfigurationProperty='conf';\" > $@",
)
