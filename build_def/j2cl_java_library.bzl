"""j2cl_java_library macro

A build macro that defines a java_library rule, a j2cl_transpile rule that
transpiles to JavaScript and a js_library referencing compiled output.
Note: All values defined on the rule are passed to the java_library rule.

Here is an example of how to use j2cl_java_library:

load("/third_party/java_src/j2cl/build_def/j2cl_java_library",
"j2cl_java_library")

j2cl_java_library(
    name = "my_name",
    srcs = glob(["*.java"]),
)

"""

load("/third_party/java_src/j2cl/build_def/j2cl_transpile", "j2cl_transpile")
load("/third_party/java_src/j2cl/build_def/js_import", "js_import")


def j2cl_java_library(add_jre_dep=True, super_srcs=[], **kwargs):
  """A macro that emits j2cl_transpile, java_library and js_library rules.

  Most callers will implicitly depend on the JRE and so 'add_jre_dep' should
  usually be left alone. But when defining the JRE itself and its sub-libraries
  it must be possible to turn off the implicit dependency to avoid a dependency
  loop.
  """

  testonly = 0
  if "testonly" in kwargs:
    testonly = kwargs["testonly"]

  java_deps = []
  js_deps = []

  if "deps" in kwargs:
    for dep in kwargs["deps"]:
      if dep == "//third_party/java/junit":
        java_deps += ["//junit/java:junit_annotations"]
        java_deps += ["//junit/java:junit_emul"]
        js_deps += ["//junit/java:junit_emul_js_library"]
      else:
        java_deps += [dep]
        js_deps += [dep + "_js_library"]
  if add_jre_dep:
    java_deps += ["//jre/java:JavaJre"]
    js_deps += ["//jre"]

  native.java_library(**kwargs)

  j2cl_transpile(
      name=kwargs["name"] + "_j2cl_transpile",
      srcs=kwargs["srcs"],
      java_library=":" + kwargs["name"],
      java_deps=java_deps,
      super_srcs=super_srcs,
      testonly=testonly,
  )

  js_library_deps = js_deps
  if add_jre_dep:
    js_library_deps += [
        "//transpiler:nativebootstrap",
        "//transpiler:vmbootstrap",
    ]
  # Bring j2cl_transpile's zip output into the js_library tree via js_import
  js_import(
      name=kwargs["name"] + "_js_import",
      srczips=[":" + kwargs["name"] + "_j2cl_transpile"],
      testonly=testonly,
  )
  native.js_library(
      name=kwargs["name"] + "_js_library",
      deps=js_library_deps + [":" + kwargs["name"] + "_js_import"],
      testonly=testonly,
  )
