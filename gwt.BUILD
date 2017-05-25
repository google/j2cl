package(default_visibility = ["//visibility:public"])

licenses(["notice"]) # Apache2

filegroup(
    name = "java_emul",
    srcs = glob(["user/super/com/google/gwt/emul/java/**/*.java"]),
)

filegroup(
    name = "java_emul_internal",
    srcs = glob(["user/super/com/google/gwt/emul/javaemul/internal/*.java"]),
)

java_library(
    name = "gwt-javaemul-internal-annotations",
    srcs = glob(["user/super/com/google/gwt/emul/javaemul/internal/annotations/*.java"]),

)
