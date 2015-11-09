"""j2cl_transpile build rule

Takes Java source and translates it into Closure style JS in a zip bundle. Java
library deps might be needed for reference resolution.


Example use:

j2cl_transpile(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    deps = [":some_dep"],
)

Note: in general you want to be using j2cl_library instead of using
j2cl_transpile directly.

"""


def _should_omit(java_file, omit_srcs):
  for _omit_src in omit_srcs:
    if java_file.path.endswith(_omit_src):
      return True
  return False


def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  omit_java_files = ctx.attr.omit_srcs  # java files whose js to ignore
  js_native_zip_files = ctx.files.native_srcs_zips
  deps = ctx.attr.deps
  dep_files = set()
  deps_paths = []
  java_files_paths = []
  omit_java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  # gather transitive files and exported files in deps
  for dep in deps:
    dep_files += dep.files
    dep_files += dep.default_runfiles.files  # for exported libraries

  # convert files to paths
  for dep_file in dep_files:
    deps_paths += [dep_file.path]

  for java_file in java_files:
    if _should_omit(java_file, omit_java_files):
      omit_java_files_paths += [java_file.path]
    java_files_paths += [java_file.path]

  js_zip_name = ctx.label.name + ".js.zip"
  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name
  ]

  if len(deps_paths) > 0:
    compiler_args += ["-cp", separator.join(deps_paths)]

  if len(omit_java_files_paths) > 0:
    compiler_args += ["-omitfiles", separator.join(omit_java_files_paths)]

  # Add the native zip file paths
  js_native_zip_files_paths = [js_native_zip_file.path for js_native_zip_file
                               in js_native_zip_files]
  if js_native_zip_files_paths:
    joined_paths = separator.join(js_native_zip_files_paths)
    compiler_args += ["-nativesourcezip", joined_paths]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  js_zip_artifact = ctx.new_file(js_zip_name)
  ctx.action(
      inputs=java_files + list(dep_files) + js_native_zip_files,
      outputs=[js_zip_artifact],
      executable=ctx.executable.transpiler,
      arguments=compiler_args,
      env=dict(LANG="en_US"),
  )

  return struct(
      files=set([js_zip_artifact]),
  )


"""j2cl_transpile: A J2CL transpile rule.

Args:
  srcs: Java source files to compile.
  deps: Java jar files for reference resolution.
  native_srcs_zips: JS zip files providing Foo.native.js implementations.
"""
# Private Args:
#   omit_srcs: Names of files to omit from the generated output. The files
#       will be included in the compile for reference resolution purposes but no
#       output JS for them will be kept.
#   transpiler: J2CL compiler jar to use.
j2cl_transpile = rule(
    attrs={
        "deps": attr.label_list(allow_files=FileType([".jar"])),
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=FileType([".java"]),
        ),
        "native_srcs_zips": attr.label_list(
            allow_files=FileType([".zip"]),
        ),
        "omit_srcs": attr.string_list(default=[]),
        "transpiler": attr.label(
            cfg=HOST_CFG,
            executable=True,
            allow_files=True,
            default=Label("//third_party/java/j2cl"),
        ),
    },
    implementation=_impl,
)
