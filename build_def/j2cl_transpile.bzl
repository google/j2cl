"""j2cl_transpile build rule

Takes Java source and translates it into Closure style JS in a zip bundle. Java
library deps might be needed for reference resolution.

A .depinfo file is also emitted that summarizes imports and exports for the
target. Line one is a comma separated list of names of imported
(goog.require()'d) modules. Line two is a comma separated list of names of
exported (goog.module() declared) modules.

Example use:

j2cl_transpile(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    deps = [":some_dep"],
)

Note: in general you want to be using j2cl_library instead of using
j2cl_transpile directly.

"""


def _get_message(ctx):
  _MESSAGES = [
    "Re" + "ti" + "cu" + "la" + "ti" + "ng" + "Sp" + "li" + "ne" + "s",
    3 * ("\\" + "0" + "/ "),
    "Co" + "mp" + "ut" + "in" + "g " + "PI",
    "So" + " m" + "uch" + " Ja" + "va",
    "Ma" + "ki" + "ng"  + " c" + "of" + "fee",
    "So" + "lv" + "in" + "g " + "ha" + "lt" + "ing" + " p" + "ro" + "bl" + "em",
    "Ex" + "ec" + "ut" + "in" + "g " + "bu" + "sy" + " l" + "oo" + "p",
    "En" + "te" + "ri" + "ng" + " w" + "ar" + "p " + "sp" + "ee" + "d"
  ]
  index = len(ctx.attr.srcs) + len(ctx.configuration.bin_dir.path)
  return _MESSAGES[index % len(_MESSAGES)] + " %s" % ctx

def _impl(ctx):
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  js_native_zip_files = ctx.files.native_srcs_zips
  deps = ctx.attr.deps
  dep_files = set()
  deps_paths = []
  java_files_paths = []
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
    java_files_paths += [java_file.path]

  js_zip_name = ctx.label.name + ".js.zip"
  js_zip_artifact = ctx.new_file(js_zip_name)
  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name,
  ]

  depinfo_name = ctx.label.name + ".depinfo"
  dependency_info_artifact = ctx.new_file(depinfo_name)
  compiler_args += ["-depinfo", ctx.configuration.bin_dir.path + "/" +
                    ctx.label.package + "/" + depinfo_name]

  if len(deps_paths) > 0:
    compiler_args += ["-cp", separator.join(deps_paths)]

  # Add the native zip file paths
  js_native_zip_files_paths = [js_native_zip_file.path for js_native_zip_file
                               in js_native_zip_files]
  if js_native_zip_files_paths:
    joined_paths = separator.join(js_native_zip_files_paths)
    compiler_args += ["-nativesourcezip", joined_paths]

  # Generate readable_maps
  if ctx.attr.readable_source_maps:
    compiler_args += ["-readableSourceMaps"]

  # Emit goog.module.declareLegacyNamespace(). This is a temporary measure
  # while onboarding Docs, do not use.
  if ctx.attr.declare_legacy_namespace:
    compiler_args += ["-declareLegacyNamespace"]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  ctx.action(
      progress_message = _get_message(ctx),
      inputs=java_files + list(dep_files) + js_native_zip_files,
      outputs=[js_zip_artifact, dependency_info_artifact],
      executable=ctx.executable.transpiler,
      arguments=compiler_args,
      env=dict(LANG="en_US.UTF-8"),
  )

  return struct(
      files=set([js_zip_artifact, dependency_info_artifact])
  )


"""j2cl_transpile: A J2CL transpile rule.

Args:
  srcs: Source files (.java or .srcjar) to compile.
  deps: Java jar files for reference resolution.
  native_srcs_zips: JS zip files providing Foo.native.js implementations.
"""
# Private Args:
#   transpiler: J2CL compiler jar to use.
j2cl_transpile = rule(
    attrs={
        "deps": attr.label_list(allow_files=FileType([".jar"])),
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=FileType([".java", ".srcjar"]),
        ),
        "native_srcs_zips": attr.label_list(
            allow_files=FileType([".zip"]),
        ),
        "readable_source_maps": attr.bool(default=False),
        "declare_legacy_namespace": attr.bool(default=False),
        "transpiler": attr.label(
            cfg=HOST_CFG,
            executable=True,
            allow_files=True,
            default=Label("//third_party/java/j2cl:J2clTranspiler"),
        ),
    },
    implementation=_impl,
    # Declare each output artifact by name, otherwise they can not be
    # referenced by name when being used as inputs for other rules.
    outputs={
        "zip_file": "%{name}.js.zip",
        "depinfo_file": "%{name}.depinfo",
    }
)
