# Apache2
licenses(["notice"])

java_library(
    name = "generator_lib",
    srcs = ["TestCasesGenerator.java"],
    runtime_deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
    deps = [
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
    ],
)

java_binary(
    name = "generator",
    main_class = "com.google.j2cl.transpiler.integration.allsimplebridges.generator.TestCasesGenerator",
    runtime_deps = ["generator_lib"],
)
