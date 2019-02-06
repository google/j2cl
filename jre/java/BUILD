# Description:
#   j2cl/jre: JRE subset for transpilation.
#

package(default_visibility = [
    "//:__pkg__",
    "//build_defs/internal_do_not_use:__pkg__",
    "//jre/javatests:__pkg__",
    "//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__",
])

licenses(["notice"])  # Apache 2.0

load(":j2cl_mirror_from_gwt.bzl", "j2cl_mirror_from_gwt")

# This target includes part of the JRE that is emulated by J2CL. We don't use it
# as the 'bootclasspath' since doing so would have multiple implications:
#
# 1. Non-JRE deps would need to be included in the bootclasspath. As result javaemul.internal can
# no longer be visibility controlled and user code will compile# without depending
# jsinterop.annotations (which will be diverge from java_library).
#
# 2. JRE still needs to be included in JS deps and achieving that without including JRE
# twice in the classpath would introduce a lot of new complexity.
#
# So instead; we have chosen a bootclasspath that is not composed of emulated JRE classes. Instead
# it is composed of only classes that are not emulated but their byte-code is still needed to make
# javac happy. This setup provides following benefits:
#
# 1. Still takes real JDK out of bootclasspath and solves all the issues that comes with it.
# 2. We no longer transpile the classes that are only required for byte-code.
# 3. Since JRE is included as a regular dep we get folllowing for free:
#  - Generated JS is included in the deps
#  - JRE emulation bytecode is included only once
#  - JRE emulation gets JavaBuilder i.jar optimizations.

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

# Target includes classes that are referenced due to synthesized byte code and only included to
# satisfy the compiler checks. They are not available as part of the JRE emulation.
# See above for more details on this setup.
java_library(
    name = "jre_bootclasspath",
    srcs = glob(["javasynth/**"]),
)
