# Description:
#   j2cl/jre: JRE subset for transpilation.
#

load(":j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

package(
    default_visibility = [
        "//:__pkg__",
        "//build_defs/internal_do_not_use:__pkg__",
        "//jre/javatests:__pkg__",
        "//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

j2cl_mirror_from_gwt(
    name = "jre",
    excluded_srcs = ["javasynth/**"],
    extra_js_srcs = [
        "//jre/java/javaemul/internal/vmbootstrap/primitives",
    ],
    mirrored_files = [
        "//third_party:gwt_java_emul",
        "//third_party:gwt_java_emul_internal",
    ],
    deps = [
        "//third_party:closure_math_long",
        "//third_party:closure_reflect",
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

# In addition to JRE emulation, this target includes classes that are referenced
# due to synthesized byte code and only included to satisfy the compiler checks
# Note that this target is separately compiled as a java_library to avoid circular
# depedency.
java_library(
    name = "jre_bootclasspath",
    srcs = glob(["javasynth/**"]) + [":jre_java_files"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)
