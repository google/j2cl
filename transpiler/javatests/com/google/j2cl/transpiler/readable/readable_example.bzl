"""readable_example build macro

Confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
readable_example(
    name = "foobar",
    srcs = glob(["*.java"]),
)

"""


load("/third_party/java_src/j2cl/build_def/j2cl_util", "J2CL_OPTIMIZED_DEFS")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")
load("/tools/build_rules/build_test", "build_test")


def readable_example(
    name, srcs, supporting_srcs=[], native_srcs=[],
    deps=[], js_deps=[], plugins=[], javacopts=[],  test_externs_list=None,
    _declare_legacy_namespace=False):
  """Macro that confirms the JS compilability of some transpiled Java.

  deps are Labels of j2cl_library() rules. NOT labels of
  java_library() rules.

  Args:
    name: The name of the readable example to generate.
    srcs: Source files to make readable output for.
    supporting_srcs: Source files referenced by the primary srcs.
    native_srcs: Foo.native.js files to merge in.
    deps: J2CL libraries referenced by the srcs.
    js_deps: JS libraries referenced by the srcs.
    plugins: APT processors to execute when generating readable output.
    javacopts: Custom opts to pass to the Java library rule.
    test_externs_list: Custom externs to support build test verification.
    _declare_legacy_namespace: Whether to use legacy namespaces in output.
  """

  if supporting_srcs:
    j2cl_library(
        name="supporting_" + name,
        srcs=supporting_srcs,
        javacopts=[
            "-Xep:SelfEquals:OFF",  # See go/self-equals-lsc
            "-Xep:IdentityBinaryExpression:OFF",
        ] + javacopts,
        native_srcs=native_srcs,
        deps=deps,
        plugins=plugins,
        generate_build_test=False,
        _js_deps=js_deps,
        _declare_legacy_namespace=_declare_legacy_namespace,
    )

  # Transpile the Java files.
  j2cl_library(
      name=name,
      srcs=srcs,
      javacopts=javacopts,
      native_srcs=native_srcs,
      deps=deps + (["supporting_" + name] if supporting_srcs else []),
      plugins=plugins,
      generate_build_test=False,
      _js_deps=js_deps,
      _readable_source_maps=True,
      _declare_legacy_namespace=_declare_legacy_namespace,
  )

  # Verify compilability of generated JS.
  if not test_externs_list:
    test_externs_list=["//javascript/externs:common"]
  native.js_binary(
      name=name + "_binary",
      defs=J2CL_OPTIMIZED_DEFS + ["--summary_detail_level=3"],
      compiler="//javascript/tools/jscompiler:head",
      externs_list=test_externs_list,
      deps=[":" + name],
  )

  build_test(
      name=name + "_build_test",
      targets=[name + "_binary"],
  )
