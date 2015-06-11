"""j2cl_java_library macro

A build macro that defines a java_library rule, a j2cl_transpile rule that
transpiles to JavaScript and a js_library referencing compiled output.
Note: All values defined on the rule are passed to the java_library rule.

Here is an example of how to use j2cl_java_library:

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")

j2cl_java_library(
    name = "my_name",
    srcs = glob(["*.java"]),
)

"""

load("/third_party/java_src/j2cl/build_def/j2cl_transpile", "j2cl_transpile")

def j2cl_java_library(**kwargs):
  """A macro that emits j2cl_transpile, java_library and js_library rules."""
  testonly = 0
  if "testonly" in kwargs:
    testonly = kwargs["testonly"]

  java_deps = []
  js_deps = []

  if "deps" in kwargs:
    for dep in kwargs["deps"]:
      if dep == "//third_party/java/junit":
        java_deps += ["//junit/java/org/junit:junit_emul"]
        # TODO(dankurka) once we have decided how we deal with junit sort this out
      else:
        java_deps += [dep]
        js_deps += [dep + "_js"]

  native.java_library(**kwargs)

  j2cl_transpile(
      name = kwargs["name"]  + "_j2cl_transpile",
      srcs = kwargs["srcs"],
      java_library = ":" + kwargs["name"],
      java_deps = java_deps,
      testonly = testonly,
  )

  native.js_library(
      name = kwargs["name"]  + "_js_library",
      srcs = [":" + kwargs["name"]  + "_j2cl_transpile",],
      deps = js_deps + ["//jre"],
      testonly = testonly,
  )
