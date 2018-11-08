licenses(["notice"])  # Apache 2.0

java_library(
    name = "generator_lib",
    srcs = ["TestCasesGenerator.java"],
    runtime_deps = ["//third_party:gwt-jsinterop-annotations"],
    deps = ["//third_party:guava"],
)

java_binary(
    name = "generator",
    main_class = "com.google.j2cl.transpiler.integration.allsimplebridges.generator.TestCasesGenerator",
    runtime_deps = ["generator_lib"],
)
