# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = [
    "//:__subpackages__",
    "//third_party/java/j2cl:__pkg__",
])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_source_copy.bzl", "j2cl_source_copy")
load("//build_def:jsni_to_j2cl_converter.bzl", "jsni_to_native_js_bundle")

J2CL_SPECIFIC_JRE_SRC = [
    "java/lang/Class.java",
    "java/lang/Enum.java",
    "java/lang/Object.java",
    "java/util/InternalJsMap.java",
    "java/util/InternalJsMapFactory.java",
    "java/lang/invoke/MethodHandles.java",
]

JRE_SRC = J2CL_SPECIFIC_JRE_SRC + [":emul"]

j2cl_source_copy(
    name = "emul",
    srcs = [
        "//third_party/java_src/gwt/svn/trunk/user:java_emul",
        "//third_party/java_src/gwt/svn/trunk/user:java_emul_internal",
    ],
    # Java classes that are different in j2cl and thus should not be copied from GWT
    excludes = J2CL_SPECIFIC_JRE_SRC,
)

jsni_to_native_js_bundle(
    name = "native_js_bundle",
    srcs = JRE_SRC,
    native_srcs = glob(["**/*.native.js"]),
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

# Handrolled JRE classes
js_library(
    name = "jre_handrolled_js",
    srcs = glob(
        ["**/*.js"],
        exclude = ["**/*.native.js"],
    ),
    deps = [
        "//javascript/closure/reflect",
        "//transpiler/nativebootstrap",
        "//transpiler/vmbootstrap",
    ],
)
