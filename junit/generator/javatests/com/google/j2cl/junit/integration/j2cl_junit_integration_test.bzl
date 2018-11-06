"""Helper for j2cl junit integration tests."""

load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package")

def j2cl_test_integration_test(name, test_data, test_data_java_only = [], deps = [], extra_data = []):
    test_data_java = test_data + test_data_java_only
    test_data_j2cl = [d + "-j2cl" for d in test_data]
    test_data_j2cl_compiled = [d + "-j2cl_compiled" for d in test_data]

    test_data_all = test_data_java + test_data_j2cl + test_data_j2cl_compiled

    native.java_test(
        name = name,
        srcs = [name + ".java"],
        data = test_data_all + extra_data,
        shard_count = len(test_data_all),
        deps = deps + [
            "//java/com/google/common/base",
            "//third_party/java/junit",
            "//third_party/java/truth",
            "//junit/generator/javatests/com/google/j2cl/junit/integration:junit_integration_helper",
        ],
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
    "//junit/generator/javatests/com/google/j2cl/junit/integration/testlogger:testlogger",
]

def j2cl_test_integration_test_data(name, deps = [], extra_defs = [], _js_deps = [], native_srcs = []):
    srcs = ["%s.java" % name]
    test_class = "%s.%s" % (get_java_package(native.package_name()), name)
    tags = ["manual", "notap"]

    # Note that, this setup also verifies that we can have our suite defined in
    # runtime deps. The build rules have to forward the runtime deps as a
    # dependency to code generation, otherwise we would be getting strict deps
    # violations. If this setup is changed, we should add explicit examples.
    java_and_j2cl_library(
        name = "%s-lib" % name,
        srcs = srcs,
        tags = tags,
        deps = deps,
        _js_deps = _js_deps,
        native_srcs = native_srcs,
    )

    j2cl_test(
        name = "%s-j2cl_compiled" % name,
        compile = 1,
        jvm_flags = JVM_FLAGS,
        tags = tags,
        test_class = test_class,
        runtime_deps = [":%s-lib-j2cl" % name],
        extra_defs = extra_defs,
    )

    j2cl_test(
        name = "%s-j2cl" % name,
        jvm_flags = JVM_FLAGS,
        tags = tags,
        test_class = test_class,
        runtime_deps = [":%s-lib-j2cl" % name],
        extra_defs = extra_defs,
    )

    native.java_test(
        name = name,
        tags = tags,
        runtime_deps = [":%s-lib" % name],
    )

def java_and_j2cl_library(
        name,
        srcs,
        _js_deps = [],
        tags = [],
        super_srcs = None,
        deps = [],
        native_srcs = []):
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
        srcs = j2cl_srcs,
        deps = j2cl_deps,
        testonly = 1,
        tags = tags,
        exports = j2cl_deps,
        _js_deps = _js_deps,
        native_srcs = native_srcs,
    )
