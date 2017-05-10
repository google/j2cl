# Google owns the copyright
licenses(["unencumbered"])

js_library(
    name = "A-native",
    srcs = [
        "a-native.js",
    ],
    deps = [
        "//javascript/closure:base",
    ],
)

# Fails with:
# ERROR: Source file "[blaze-out/gcc-4.X.Y-crosstool-v18-hybrid-grtev4-k8-fastbuild/genfiles/]third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/buildtest/ajd_prune_deps/C.js.zip!/com/google/j2cl/transpiler/buildtest/ajd_prune_deps/C.java.js" requires namespace "a.A$$Overlay", but no file was found in the dependencies of this rule that provides the latter.
# j2cl_library(
#     name = "C",
#     srcs = [
#         "C.java",
#     ],
#     js_deps_mgmt = "closure",
#     deps = [
#         ":B",
#     ],
# )
