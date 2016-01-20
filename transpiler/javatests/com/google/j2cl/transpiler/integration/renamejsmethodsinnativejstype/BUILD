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
    name = "renamejsmethodsinnativejstype",
    # Src's JUST the Main class and gets Foo as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = ["Main.java"],
    js_deps = [":native_types"],
    native_srcs = ["Main.native.js"],
    deps = [
        ":Foo",
        "//jre/java:gwt-jsinterop-annotations",
    ],
)

js_library(
    name = "native_types",
    srcs = ["NativeFoo.js"],
)
