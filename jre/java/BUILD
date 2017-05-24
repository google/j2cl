# Description:
#   j2cl/jre: JRE subset for transpilation.
#
# Can depend on the native bootstrap JS but not on the VM opcode JS.

package(default_visibility = [
    "//third_party/java/j2cl:__subpackages__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

j2cl_mirror_from_gwt(
    name = "jre",
    extra_js_srcs = [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
    js_deps = [
        "//javascript/closure/reflect",
        "//javascript/closure/math:long",
        "//javascript/closure:base",
    ],
    mirrored_files = [
        "//third_party/java_src/gwt/svn/trunk/user:java_emul",
        "//third_party/java_src/gwt/svn/trunk/user:java_emul_internal",
    ],
    deps = [
        "//third_party/java/gwt:gwt-javaemul-internal-annotations-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/jsr305_annotations:jsr305_annotations-j2cl",
    ],
)
