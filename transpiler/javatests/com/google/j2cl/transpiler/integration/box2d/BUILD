package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

gwt_module(
    name = "box2d_gwt",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/collect:collect-gwt",
        "//third_party/java/jbox2d:jbox2d_gwt",
    ],
)
