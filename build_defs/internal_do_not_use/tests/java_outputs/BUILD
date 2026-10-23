load("@rules_java//java:defs.bzl", "java_plugin", "java_test")
load("//build_defs:rules.bzl", "j2cl_library")
load(":outputs.bzl", "java_outputs", "javascript_output")

java_plugin(
    name = "resource_processor",
    srcs = ["ResourceProcessor.java"],
    generates_api = True,
    processor_class = "example.ResourceProcessor",
)

j2cl_library(
    name = "subject",
    srcs = ["Example.java"],
    generate_build_test = False,
    generate_j2wasm_library = False,
    plugins = [":resource_processor"],
)

java_outputs(
    name = "java_outputs",
    library = ":subject",
)

javascript_output(
    name = "javascript_output",
    library = ":subject",
)

java_test(
    name = "JavaOutputTest",
    srcs = ["JavaOutputTest.java"],
    data = [
        ":java_outputs",
        ":javascript_output",
    ],
    jvm_flags = [
        "-Djava.output=$(rootpath :java_outputs)",
        "-Djavascript.output=$(rootpath :javascript_output)",
    ],
    test_class = "example.JavaOutputTest",
    deps = ["//third_party:junit"],
)
