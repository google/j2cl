load("//build_defs:rules.bzl", "j2cl_import")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = ["Foo.java"],
    plugins = [":dummy-processor"],
)

# Wrap up the APT processor as a library.
java_library(
    name = "dummy-processor-lib",
    srcs = ["DummyProcessor.java"],
)

# Provide a java_plugin interface for that APT processor library.
java_plugin(
    name = "dummy-processor",
    processor_class = "aptgeneratorplugin.DummyProcessor",
    deps = [":dummy-processor-lib"],
)

# Re-export it via a java_library target so that users can just dep on it
# instead of using a java_plugin attribute.
java_library(
    name = "dummy-processor-export",
    exported_plugins = [":dummy-processor"],
    exports = [":dummy-processor-lib"],
)

# Expose that re-exporting java_library to the J2CL zone.
j2cl_import(
    name = "dummy-processor-export-j2cl",
    jar = ":dummy-processor-export",
)
