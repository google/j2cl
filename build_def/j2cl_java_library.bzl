"""j2cl_java_library build macro

Takes Java source, translates it into Closure style JS and surfaces it to the
rest of the build tree as a js_library(). Generally library rules dep on other
library rules for reference resolution and this build macro is no exception.
In particular the deps this rule needs for reference resolution are
java_library() targets which will have been created by other invocations of
this same j2cl_java_library build macro.


Example use:

j2cl_java_library(
    name = "my_name",
    srcs = glob(["*.java"]),
)

"""


load("/third_party/java_src/j2cl/build_def/j2cl_transpile", "j2cl_transpile")


def _js_import_impl(ctx):
  use_ajd = False
  js_provider = js_common.provider(
      ctx, ctx.files.srczips, [dep.js for dep in ctx.attr.deps], use_ajd)
  return struct(
      files=set(js_provider.depgraphs()),
      js=js_provider,
  )


_js_import = rule(
    implementation=_js_import_impl,
    attrs={
        "deps": attr.label_list(
            allow_files=False,
            providers=["js"]),
        "srczips": attr.label_list(
            allow_files=FileType([".js.zip"])),
    },
)


def j2cl_java_library(native_sources_zips=[],
                      _add_standard_library_dep=True,
                      _omit_srcs=[],
                      **kwargs):
  """Translates Java source into JS source in a js_library() target.

  Args:
    native_sources_zips: Zip files providing Foo.native.js files.
  """
  # Private Args:
  #   _add_standard_library_dep: Whether to implicitly add a dependency on the
  #       standard library.
  #   _omit_srcs: Names of files to omit from the generated output. The files
  #       will be included in the compile for reference resolution purposes but
  #       no output JS for them will be kept.

  testonly = 0
  if "testonly" in kwargs:
    testonly = kwargs["testonly"]

  java_deps = []
  js_deps = []

  if "deps" in kwargs:
    for dep in kwargs["deps"]:
      if dep == "//third_party/java/junit":
        java_deps += [
            "//junit/opensource/java:junit_emul"
        ]
        js_deps += [
            "//junit/opensource/java:junit_emul_js_library"
        ]
      else:
        java_deps += [dep]
        js_deps += [dep + "_js_library"]
  if _add_standard_library_dep:
    java_deps += ["//jre/java:JavaJre"]
    js_deps += ["//jre"]

  native.java_library(**kwargs)

  j2cl_transpile(
      name=kwargs["name"] + "_j2cl_transpile",
      srcs=kwargs["srcs"],
      deps=java_deps,
      native_sources_zips=native_sources_zips,
      testonly=testonly,
      omit_srcs=_omit_srcs,
  )

  js_library_deps = js_deps
  if _add_standard_library_dep:
    js_library_deps += [
        "//transpiler:nativebootstrap",
        "//transpiler:vmbootstrap",
    ]
  # Bring j2cl_transpile's zip output into the js_library tree via _js_import
  _js_import_name = kwargs["name"] + "_js_import"
  _js_import(
      name=_js_import_name,
      srczips=[":" + kwargs["name"] + "_j2cl_transpile"],
      testonly=testonly,
  )

  native.js_library(
      name=kwargs["name"] + "_js_library",
      deps=js_library_deps + [":" + _js_import_name],
      testonly=testonly,
  )
