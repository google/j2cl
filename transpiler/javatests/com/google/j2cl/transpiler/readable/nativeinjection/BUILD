package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "nativeinjection",
    srcs = glob(["*.java"]),
    native_sources_zips = [":native_files.zip"],
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
