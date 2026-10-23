load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:defs.bzl", "java_library", "java_plugin")
load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library")

java_library(
    name = "dependency",
    srcs = [
        "Dependency.java",
        "TypeUse.java",
    ],
)

java_plugin(
    name = "processor",
    srcs = ["DependencyProcessor.java"],
    processor_class = "example.DependencyProcessor",
)

j2cl_import(
    name = "dependency-j2cl",
    jar = ":dependency",
)

j2cl_library(
    name = "subject",
    srcs = ["Subject.java"],
    generate_build_test = False,
    generate_j2wasm_library = False,
    javacopts = ["-XDaddTypeAnnotationsToSymbol=true"],
    plugins = [":processor"],
    deps = [":dependency-j2cl"],
)

build_test(
    name = "dependency_type_annotations_test",
    targets = [":subject"],
)
