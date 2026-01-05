"""Helper for creating test bundles for JRE."""

load("@rules_java//java:defs.bzl", "java_binary")
load("//jre/java:jdk_system.bzl", "jdk_system")

def jre_test_bundle(name, bootclasspath):
    # A java_binary to create a merged jar for transitive closure of bootclasspath.
    java_binary(
        name = name,
        create_executable = 0,
        runtime_deps = [bootclasspath],
    )

    # JDK system module for the JRE bootclasspath.
    jdk_system(
        name = name + "_system",
        bootclasspath = bootclasspath,
    )

    native.filegroup(
        name = name + "_files",
        srcs = [":" + name + "_system", ":" + name + "_deploy.jar", ":" + name + "_deploy-src.jar"],
    )
