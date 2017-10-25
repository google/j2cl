# Apache2
licenses(["notice"])

java_library(
    name = "transpiler_lib",
    srcs = ["J2clTranspiler.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__"],
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
    ],
)

java_binary(
    name = "J2clTranspiler",
    jvm_flags = ["-XX:+TieredCompilation"],
    main_class = "com.google.j2cl.transpiler.J2clTranspiler",
    runtime_deps = [":transpiler_lib"],
)

java_binary(
    name = "J2clTranspiler_worker",
    jvm_flags = ["-XX:+TieredCompilation"],
    main_class = "com.google.j2cl.transpiler.J2clTranspilerWorker",
    visibility = ["//internal_do_not_use:__pkg__"],
    runtime_deps = [":transpiler_worker_lib"],
)
