package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")
load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/integration_test",
    "integration_test",
)

j2cl_java_library(
    name = "Foo",
    # Src's JUST the Foo class.
    srcs = ["Foo.java"],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations",
    ],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "renamejsmethodsinnativejstype",
    # Src's JUST the Main class and gets Foo as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = ["Main.java"],
    js_deps = [":native_types"],
    native_sources_zips = [":native_files.zip"],
    deps = [
        ":Foo",
        "//third_party/java/gwt:gwt-jsinterop-annotations",
    ],
)

genzip(
    name = "native_files.zip",
    deps = [":native_files_pkg_library"],
)

pkg_library(
    name = "native_files_pkg_library",
    srcs = glob(["native_sources/**/*.js"]),
    flatten = 1,
    package_dir = "com/google/j2cl/transpiler/integration/renamejsmethodsinnativejstype",
)

js_library(
    name = "native_types",
    srcs = ["NativeFoo.js"],
)
