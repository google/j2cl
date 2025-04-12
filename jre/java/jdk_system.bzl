"""Helpers to create a JDK system module."""

load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")

def _jdk_system(ctx):
    # TODO(goktug): Use header jar for open-source when Bazel with new Turbine is available.
    bootclasspath = ctx.file.bootclasspath

    system = ctx.actions.declare_directory("%s" % ctx.label.name)
    java_runtime = ctx.attr._runtime[java_common.JavaRuntimeInfo]
    zip_tool = ctx.executable._zip

    ctx.actions.run_shell(
        inputs = depset([bootclasspath], transitive = [java_runtime.files]),
        outputs = [system],
        tools = [zip_tool],
        command = "\n".join([
            "set -eu",
            "JAVABASE=%s" % java_runtime.java_home,
            "MODULE_DIR=%s" % system.path,
            "WD=$(pwd)",
            "TMP=$(mktemp -d)",
            'VERSION=$("${JAVABASE}/bin/jlink" --version)',
            'rm -rf "${MODULE_DIR}"',
            'mkdir "${TMP}/classes" "${TMP}/jmod"',
            '(cd ${TMP}/classes && "${WD}/%s" x "${WD}/%s")' % (zip_tool.path, bootclasspath.path),
            '"${JAVABASE}/bin/jmod" create \\',
            "  --module-version ${VERSION} \\",
            "  --target-platform linux-amd64 \\",
            '  --class-path "${TMP}/classes" \\',
            '  "${TMP}/jmod/module.jmod"',
            '"${JAVABASE}/bin/jlink" \\',
            '    --module-path "${TMP}/jmod" \\',
            "    --add-modules java.base \\",
            "    --disable-plugin system-modules \\",
            '    --output "${MODULE_DIR}"',
            'cp "${JAVABASE}/lib/jrt-fs.jar" "${MODULE_DIR}/lib/"',
            'rm -rf "${TMP}" || true',
        ]),
    )

    # Collect the dependency jars for bootclasspath, skipping the first element which is the
    # bootclasspath jar itself.
    # Bootclasspath deps are needed downstream to resolve annotations.
    deps = ctx.attr.bootclasspath[JavaInfo].transitive_compile_time_jars.to_list()[1:]

    return [
        java_common.BootClassPathInfo(
            system = system,
            bootclasspath = [bootclasspath],
            auxiliary = deps,
        ),
        DefaultInfo(files = depset([system, bootclasspath])),
    ]

jdk_system = rule(
    implementation = _jdk_system,
    attrs = {
        "bootclasspath": attr.label(
            cfg = "target",
            allow_single_file = True,
            providers = [JavaInfo],
        ),
        "_runtime": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"),
            cfg = "exec",
            providers = [java_common.JavaRuntimeInfo],
        ),
        "_zip": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("@bazel_tools//tools/zip:zipper"),
        ),
    },
)
