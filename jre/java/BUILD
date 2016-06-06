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
        "//javascript/closure/math:long",
        "//javascript/closure:base",
    ],
    mirrored_files = [
        "//third_party/java_src/gwt/svn/trunk/user:java_emul",
        "//third_party/java_src/gwt/svn/trunk/user:java_emul_internal",
    ],
    deps = [":gwt-jsinterop-annotations"],
)

# This is very hacky and done like this to avoid branching
# GWT/J2CL from our external repo and google3.
#
# java_library does not allow for Java8 sources right now.
# However for Java8 day GWT and J2CL need Java8 idioms in
# their standard runtime. For GWT this is not an issue, since
# that code is never seen by javac, but in J2CL we actually
# have to have a jar with .class files for our own emulation.
# This is why we use javac here directly so that we can compile
# this one library that actually contains Java 8 idioms.
# Note: All of this will go away after Java8 day.

load("//third_party/java/j2cl:j2cl_library.bzl", "js_import")
load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")
load("//build_def:jsni_to_native_js_bundle.bzl", "jsni_to_native_js_bundle")

# Prepare the 'jre' java_library target

# Since we can not rely on blaze to resolve deps for us
# we use a java_binary to collect all deps for the compilation
# into one jar
java_binary(
    name = "jre_deps",
    create_executable = 0,
    runtime_deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

# commands needed to compile sources and build a jar
commands = [
    "set -e",
    "mkdir compile_out",
    "third_party/java/jdk/jdk8-google-v7-64/bin/javac -source 8 -target 8 -d compile_out" +
    " -cp $(location :jre_deps_deploy.jar)" +
    " $(SRCS)",
    "cd compile_out/",
    "zip -r tmp.jar *",
    "cd ..",
    "cp compile_out/tmp.jar $(OUTS)",
]

genrule(
    name = "compile_jre",
    srcs = [":jre_hidden_java_files"],
    outs = ["jre8.jar"],
    cmd = "\n".join(commands),
    tools = [
        ":jre_deps_deploy.jar",
        "//third_party/java/jdk:jdk8-google-v7-64",
    ],
)

java_import(
    name = "jre_java_library",
    jars = ["jre8.jar"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

# Prepare the 'jre' JavaScript target

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

js_import(
    name = "jre_import",
    srczips = [
        ":jre_transpiled",
        ":jre_hidden_handrolled.js.zip",
    ],
)

# Bundles together JRE code with its deps.
js_library(
    name = "jre",
    deps = [
        ":jre_import",
        "//javascript/closure/reflect",
        "//javascript/closure/math:long",
        "//javascript/closure:base",
    ],
)
