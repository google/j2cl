# Description:
#   j2cl/tools: j2cl tools.

package(
    default_visibility = [
        "//java/com/google/protobuf/contrib/gwt:__pkg__",
        "//:__subpackages__",
    ],
)

# Google owns the copyright
licenses(["unencumbered"])

# Applications
# =========================================================
java_binary(
    name = "jsni2js",
    main_class = "com.google.j2cl.tools.jsni.JsniConverter",
    runtime_deps = [
        "//tools/java/com/google/j2cl/tools/jsni:jsniConverter",
    ],
)
