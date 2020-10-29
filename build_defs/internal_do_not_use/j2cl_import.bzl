"""j2cl_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2cl_import() target to be directly depended upon from j2cl_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!


Example use:

j2cl_import(
  name = "Qux",
  jar = "//java/com/qux:qux_java",
)

j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
    deps = [":Qux"],  # the j2cl_import target
)

"""

load(":j2cl_java_library.bzl", "j2cl_java_import")
load(":j2wasm_import.bzl", "j2wasm_import")
load(":j2wasm_library.bzl", "to_j2wasm_name")

def j2cl_import(
        name,
        jar,
        visibility = None,
        **kwargs):
    """Translates nonstandard inputs into a dep'able j2cl_library.

    Args:
      jar: Jar file to appropriately rename.
    """

    j2cl_java_import(
        name = name,
        jar = jar,
        visibility = visibility,
        **kwargs
    )

    j2wasm_args = dict(kwargs)
    j2wasm_args["tags"] = (j2wasm_args.get("tags") or []) + ["manual", "notap"]

    j2wasm_import(
        name = to_j2wasm_name(name),
        jar = jar,
        visibility = visibility,
        **j2wasm_args
    )
