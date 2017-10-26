# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//internal_do_not_use:__pkg__",
    "//jre/javatests:__pkg__",
    "//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__",
])

# Apache2
licenses(["notice"])

load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "jre",
    extra_js_srcs = [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
    js_deps = [
        "//third_party:closure_reflect",
        "//third_party:closure_math_long",
        "//third_party:closure_base",
    ],
    mirrored_files = [
        "//third_party:gwt_java_emul",
        "//third_party:gwt_java_emul_internal",
    ],
    deps = [
        "//third_party/java/jsr305_annotations:jsr305_annotations-j2cl",
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
