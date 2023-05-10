"""Helper for j2cl junit integration tests."""

load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test", "j2kt_jvm_test", "j2kt_native_test", "j2wasm_test")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

def j2cl_test_integration_test(name, test_data, test_data_java_only = [], deps = [], extra_data = [], platforms = ["CLOSURE"], tags = []):
    """Run tests against integration test data

    Args:
        name: The name of the test.
        test_data: The integration test target/files.
        test_data_java_only: Test data for java.
        deps: Dependencies for this target.
        extra_data: Files needed by this rule at runtime.
        platforms: The platform on which tests run.
        tags: Tags to be passed to the underlying test target.
    """
    test_data_java = test_data + test_data_java_only
    test_data_j2cl = [d + "-j2cl" for d in test_data]
    test_data_j2cl_compiled = [d + "-j2cl_compiled" for d in test_data]
    test_data_j2kt = [d + "-j2kt-jvm" for d in test_data]
    test_data_j2wasm = [d + "-j2wasm" for d in test_data]
    test_data_j2wasm_optimized = [d + "-j2wasm_optimized" for d in test_data]

    test_data_all = test_data_java + test_data_j2cl + test_data_j2cl_compiled
    if "WASM" in platforms:
        test_data_all = test_data_all + test_data_j2wasm + test_data_j2wasm_optimized

    if "J2KT" in platforms:
        test_data_all = test_data_all + test_data_j2kt

    shard_count = len(test_data_all)
    if shard_count > 50:
        fail("Attempted to run %d test cases, which exceeds max shards of 50. Manually split test %s into fewer tests" % (shard_count, name))

    native.java_test(
        name = name,
        srcs = [name + ".java"],
        data = test_data_all + extra_data,
        shard_count = shard_count,
        deps = deps + [
            "//java/com/google/common/base",
            "//third_party/java/junit",
            "//third_party/java/truth",
            "//junit/generator/javatests/com/google/j2cl/junit/integration:junit_integration_helper",
        ],
        tags = [
            "j2cl",
            "requires-net:external",
        ] + tags,
    )

# Flags needed to run the testing infrastucture in a way that outputs to stdout/stderr
JVM_FLAGS = [
    "-Dcom.google.testing.selenium.browser=CHROME_LINUX",
    "-Djava.util.logging.config.class=com.google.common.logging.ConfigurableLogConfig",
    "-Dcom.google.common.logging.properties.class=com.google.common.logging.LoggingPropertiesFromSystemProperties",
    "-Dgoogle.logPrefix=LOG",
    "-DLOG.handlers=java.util.logging.ConsoleHandler",
    "-DLOGjava.util.logging.ConsoleHandler.level=INFO",
    "-DLOG.level=INFO",
    "-Djsrunner.logging.level=INFO",
    "-Djsrunner.logging.verbose=true",
    "-DstacktraceDeobfuscation=true",
]

_DEFAULT_JAVA_DEPS = [
    "//third_party/java/junit:junit",
    "//junit/generator/javatests/com/google/j2cl/junit/integration/testing/testlogger:testlogger",
]

def j2cl_test_integration_test_data(
        name,
        deps = [],
        extra_defs = [],
        native_srcs = [],
        native_deps = [],
        platforms = ["CLOSURE"],
        enable_rta = True):
    """Generate j2cl and j2wasm integration test data

    Args:
        name: The test name.
        deps: Dependencies for this target. Generally should only list rule targets.
        extra_defs: A list of additional jscompiler flags to use when compiling the test.
        native_srcs: The native srcs.
        native_deps: Native libraries that will be built only for the target.
        platforms: The platform on which tests will run
        enable_rta: Whether to run tests with RTA pruning applied.
    """
    test_class = "%s.%s" % (get_java_package(native.package_name()), name)
    tags = ["manual", "notap"]
    java_srcs = native.glob(["%s.java" % name])
    kotlin_srcs = native.glob(["%s.kt" % name])
    if java_srcs and kotlin_srcs:
        fail("cannot handle both Java and Kotlin source at once")
    elif kotlin_srcs:
        # Note that, this setup also verifies that we can have our suite defined in
        # runtime deps. The build rules have to forward the runtime deps as a
        # dependency to code generation, otherwise we would be getting strict deps
        # violations. If this setup is changed, we should add explicit examples.
        kotlin_and_j2cl_library(
            name = "%s-lib" % name,
            srcs = kotlin_srcs,
            tags = tags,
            deps = deps,
            native_srcs = native_srcs,
            native_deps = native_deps,
        )
    else:
        # Note that, this setup also verifies that we can have our suite defined in
        # runtime deps. The build rules have to forward the runtime deps as a
        # dependency to code generation, otherwise we would be getting strict deps
        # violations. If this setup is changed, we should add explicit examples.
        java_and_j2cl_library(
            name = "%s-lib" % name,
            srcs = java_srcs,
            tags = tags,
            deps = deps,
            native_srcs = native_srcs,
            native_deps = native_deps,
        )

    if "WASM" in platforms:
        j2wasm_test(
            name = "%s-j2wasm" % name,
            jvm_flags = JVM_FLAGS,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2wasm" % name],
            extra_defs = extra_defs,
            optimize = False,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )

        j2wasm_test(
            name = "%s-j2wasm_optimized" % name,
            jvm_flags = JVM_FLAGS,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2wasm" % name],
            extra_defs = extra_defs,
            optimize = True,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )
    if "CLOSURE" in platforms:
        j2cl_test(
            name = "%s-j2cl_compiled" % name,
            compile = 1,
            jvm_flags = JVM_FLAGS,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2cl" % name],
            enable_rta = enable_rta,
            extra_defs = extra_defs,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )

        j2cl_test(
            name = "%s-j2cl" % name,
            jvm_flags = JVM_FLAGS,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2cl" % name],
            enable_rta = enable_rta,
            extra_defs = extra_defs,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )
    if "J2KT" in platforms:
        j2kt_jvm_test(
            name = "%s-j2kt-jvm" % name,
            jvm_flags = JVM_FLAGS,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2kt-jvm" % name],
            extra_defs = extra_defs,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )

        j2kt_native_test(
            name = "%s-j2kt-native" % name,
            tags = tags,
            test_class = test_class,
            runtime_deps = [":%s-lib-j2kt-native" % name],
            extra_defs = extra_defs,
            javacopts = [
                "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
            ],
        )
    native.java_test(
        name = name,
        tags = tags,
        runtime_deps = [":%s-lib" % name],
        javacopts = [
            "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
        ],
    )

def java_and_j2cl_library(
        name,
        srcs,
        deps = [],
        native_deps = [],
        tags = [],
        super_srcs = None,
        native_srcs = []):
    """Create java and j2cl library

    Args:
        name: A unique name for this target.
        srcs: The list of source files that are processed to create the target.
        deps: The list of other libraries to be linked in to the target.
        native_deps: Native libraries that will be built only for the target
        tags: List of strings.
        super_srcs: super sources files.
        native_srcs: native source files.
    """
    deps = deps + _DEFAULT_JAVA_DEPS
    j2cl_deps = [dep + "-j2cl" for dep in deps]
    j2cl_srcs = super_srcs or srcs

    native.java_library(
        name = name,
        srcs = srcs,
        deps = deps,
        testonly = 1,
        tags = tags,
        exports = deps,
    )

    j2cl_library(
        name = name + "-j2cl",
        srcs = j2cl_srcs + native_srcs,
        deps = j2cl_deps + native_deps,
        testonly = 1,
        tags = tags,
        exports = j2cl_deps,
    )
