# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = [
    "//:__subpackages__",
    "//third_party/java/j2cl:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    # TODO(goktug): rename this back to jre once we have Java8 in g3.
    name = "jre_hidden",
    extra_js_srcs = [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
    generate_build_test = False,
    js_deps = [
        "//javascript/closure/reflect",
        "//javascript/closure/log",
        "//javascript/closure/math:long",
        "//javascript/closure:base",
    ],
    mirrored_files = [
        "//third_party/java_src/gwt/svn/trunk/user:java_emul",
        "//third_party/java_src/gwt/svn/trunk/user:java_emul_internal",
    ],
    deps = [":gwt-jsinterop-annotations"],
)

load("//third_party/java/j2cl:j2cl_library.bzl", "js_import")
load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")
load("//build_def:jsni_to_native_js_bundle.bzl", "jsni_to_native_js_bundle")

# Prepare the 'jre' JavaScript target

java_library(
    name = "jre_java_library",
    srcs = [":jre_hidden_java_files"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

jsni_to_native_js_bundle(
    name = "jre_native_zips",
    srcs = [":jre_hidden_java_files"],
    native_srcs = glob(["**/*.native.js"]),
    deps = [":jre_java_library"],
)

j2cl_transpile(
    name = "jre_transpiled",
    srcs = [":jre_hidden_java_files"],
    native_srcs_zips = [":jre_native_zips"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

# The content of handrolled.depinfo file must be hand-maintained to keep in
# sync with the handrolled nativebootstrap and vmbootstrap JS files. The first
# row is a comma separated list of goog.require()d things and the second row
# is a comma separated list of goog.provide()d things.
#
# This is not a very maintainable solution. Only the JRE should go to the
# trouble to merge hand-rolled JS with a generated ZIP in one step.
# TODO(b/29509857): Replace all hand-rolled JS with Java then delete this.
js_import(
    name = "jre",
    depinfos = [
        "handrolled.depinfo",
        ":jre_transpiled.depinfo",
    ],
    srczips = [
        ":jre_hidden_handrolled.js.zip",
        ":jre_transpiled.js.zip",
    ],
    deps = [
        "//javascript/closure:base",
        "//javascript/closure/log",
        "//javascript/closure/math:long",
        "//javascript/closure/reflect",
    ],
)
