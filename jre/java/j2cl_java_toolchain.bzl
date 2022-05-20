"""Helper to create a google3 Java toolchain for J2CL compilers"""

load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")
load(":jdk_system.bzl", "jdk_system")

def j2cl_java_toolchain(name, bootclasspath, visibility = None, **kwargs):
    """Defines a `java_toolchain` for J2CL compilers.

    Args:
        name: Name of the Java toolchain

        bootclasspath: A `java_library` to use as the bootclasspath

        visibility: Visibility of the Java toolchain

        **kwargs: Args passed through to `default_java_toolchain`.
          Note: `source_version` and `target_version` are not allowed.
    """

    jdk_system_name = name + "_jdk_system"

    jdk_system(
        name = jdk_system_name,
        bootclasspath = bootclasspath,
    )

    default_java_toolchain(
        name = name + "_8",
        bootclasspath = [jdk_system_name],
        source_version = "8",
        target_version = "8",
        **kwargs
    )

    default_java_toolchain(
        name = name + "_11",
        bootclasspath = [jdk_system_name],
        source_version = "11",
        target_version = "11",
        **kwargs
    )

    native.alias(
        name = name,
        actual = select({
            "//jre/java:experimental_java11_support": ":" + name + "_11",
            "//conditions:default": ":" + name + "_8",
        }),
        visibility = visibility,
    )
