package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/integration_test",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "staticjsmethods",
    srcs = glob(["*.java"]),
    native_sources_zips = [":native_files.zip"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

genzip(
    name = "native_files.zip",
    deps = [":native_files_pkg_library"],
)

pkg_library(
    name = "native_files_pkg_library",
    srcs = glob(["native_sources/**/*.js"]),
    flatten = 0,
    strip_prefix = PACKAGE_NAME + "/native_sources",
)
