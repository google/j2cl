"""j2cl_library build macro

Takes Java source, translates it into Closure style JS and surfaces it to the
rest of the build tree as a js_library(). Generally library rules dep on other
library rules for reference resolution and this build macro is no exception.
In particular the deps this rule needs for reference resolution are
java_library() targets which will have been created by other invocations of
this same j2cl_library build macro.


Example use:

# creates js_library(name="Foo") containing translated JS.
j2cl_library(
    name = "Foo",
    srcs = glob(["Foo.java"]),
    deps = [":Bar"]  # Directly depends on j2cl_library(name="Bar")
)

# creates js_library(name="Bar") containing the results.
j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
)

"""

load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")
load("//build_def:j2cl_util.bzl", "generate_zip", "J2CL_OPTIMIZED_DEFS")
load("//tools/build_defs/label:def.bzl", "absolute_label")
load("//tools/build_defs/j2cl:def.bzl", "js_import")
load("//tools/build_defs/lib:lib.bzl", "collections")
load("//tools/build_rules:build_test.bzl", "build_test")
load("//build_def:gwt_incompatible_stripper.bzl", "gwt_incompatible_stripper")

def _do_env_copy(env_restricted_artifact, unrestricted_artifact, testonly):
  """Copies an artifact from to remove build environment restrictions."""
  native.genrule(
      name=unrestricted_artifact + "_genrule",
      tools=[env_restricted_artifact],
      outs=[unrestricted_artifact],
      testonly=testonly,
      tags=["notap", "manual"],
      cmd="cp $(location %s) $@" % env_restricted_artifact,
      local=True,
  )

def _get_absolute_labels(args, key):
  """Returns the absolute label for the provided key if exists, otherwise empty."""
  labels = args.get(key) or []
  if type(labels) != type([]):
    fail("Expected value of type 'list(label)' for attribute '%s' in j2cl_library rule" % key)

  return [absolute_label(target) for target in labels]

def _merge_zips(srczips, outzip, tags, testonly):
  """Merges provided zip files."""
  zip_tool = "$$cwd/$(location //third_party/zip:zip)"
  native.genrule(
      name=outzip + "_genrule",
      srcs=srczips,
      outs=[":" + outzip],
      cmd="\n".join([
          # Unzip the srczips into a temp dir.
          "TMPDIR=$$(mktemp -d $@.tmp.XXXXXX)",
          "for src in $(SRCS); do",
          # unzip errors out for empty zip so we ignore that with || true
          "  unzip -q $$src -d $$TMPDIR 2> /dev/null || true",
          "done",

          # Rezip the temp dir using relative paths.
          "cwd=$$PWD",
          "cd $$TMPDIR",
          # zip errors out for empty dir - a dummy dir (even excluded) prevents that.
          "mkdir __dummy__",
          "%s -jt -X -qr $$cwd/$@ . -x __dummy__" % zip_tool,
      ]),
      tools=["//third_party/zip:zip"],
      tags=tags,
      testonly=testonly,
      local=True,
  )

def j2cl_library(name,
                 srcs=[],
                 tags=[],
                 native_srcs=[],
                 native_srcs_zips=[],
                 generate_build_test=None,
                 js_deps_mgmt="closure",
                 visibility=None,
                 _js_srcs=[],
                 _js_deps=[],
                 _readable_source_maps=False,
                 _declare_legacy_namespace=False,
                 _test_externs_list=[],
                 **kwargs):
  """Translates Java source into JS source in a js_library() target.

  Implicit output targets:
    lib<name>-src.jar: A java archive containing the sources (source jar).

  Args:
    srcs: Source files (.java or .srcjar) to compile.
    native_srcs: Foo.native.js source files.
    native_srcs_zips: Zip files providing Foo.native.js files.
    deps: Labels of other j2cl_library() rules.
          NOT labels of java_library() rules.
  """
  # Private Args:
  #   _js_srcs: JavaScript source files (.js) to include in the bundle.
  #   _js_deps: Direct js_library dependencies needed by native code (either
  #       via srcs in _js_srcs or via JsInterop/native.js).
  #       For the JsInterop scenario, we encourage developers to create
  #       proper JsInterop stubs next to the js_library rule and create a
  #       j2cl_import rule there.
  #   _declare_legacy_namespace: A temporary measure while onboarding Docs, do
  #       not use.

  # exit early to avoid parse errors when running under bazel
  if not hasattr(native, "js_library"):
    return

  base_name = name
  srcs = srcs or []
  tags = tags or []
  testonly = kwargs.get("testonly")

  # Direct automated dep picking tools and grok away from internal targets.
  internal_tags = tags + ["avoid_dep", "no_grok"]
  java_exports = []
  js_exports = []

  java_deps = []
  js_deps = _js_deps

  exports = _get_absolute_labels(kwargs, "exports")
  deps = _get_absolute_labels(kwargs, "deps")

  if not srcs and deps:
    fail("deps not allowed without srcs")

  for export in exports:
    java_exports += [export + "_java_library"]
    js_exports += [export]

  gwt_incompatible_stripped = base_name + "_gwtincompatible_stripped"
  gwt_incompatible_stripper(
      name=gwt_incompatible_stripped,
      srcs=srcs,
      visibility=["//visibility:private"],
      testonly=testonly,
      tags=internal_tags,
  )

  target_name = PACKAGE_NAME + ":" + base_name
  # If this is JRE itself, don't synthesize the JRE dep.
  if srcs and target_name != "third_party/java_src/j2cl/jre/java:jre":
    deps += ["//internal_do_not_use:jre"]

  java_deps = java_deps[:]
  js_deps = js_deps[:]
  for dep in deps:
    java_deps += [dep + "_java_library"]
    js_deps += [dep]

  java_library_kwargs = dict(kwargs)
  java_library_kwargs["deps"] = java_deps or None
  java_library_kwargs["exports"] = java_exports
  java_library_kwargs["restricted_to"] = ["//buildenv/j2cl:j2cl_compilation"]

  native.java_library(
      name = base_name + "_java_library",
      srcs = [gwt_incompatible_stripped],
      tags = internal_tags,
      visibility = visibility,
      **java_library_kwargs
  )

  src_zips = []

  # TODO(dankurka): Make sure we only rely on the srcjar if we actually
  # have an APT running
  if srcs:
    # extract js files from apts
    js_sources_from_apt = base_name + "js_src_apt.zip"
    native_js_sources_from_apt = base_name + "_native_js_src_apt.zip"
    zip_tool = "$$cwd/$(location //third_party/zip:zip)"
    native.genrule(
        name=base_name + "_extract_native_js_apt",
        srcs=["lib" + base_name + "_java_library.jar"],
        outs=[js_sources_from_apt, native_js_sources_from_apt],
        restricted_to=["//buildenv/j2cl:j2cl_compilation"],
        cmd="\n".join([
            "TMPDIR=$$(mktemp -d tmp.XXXXXX)",
            "unzip -q $(SRCS) -x \"*.class\" -d $$TMPDIR",
            "cwd=$$PWD",
            "cd $$TMPDIR",
            "%s -jt -X -q -i \"*.js\" -x \"*.native.js\" -r $$cwd/$(location %s) *" % (
                zip_tool, js_sources_from_apt),
            "%s -jt -X -q -i \"*.native.js\" -r $$cwd/$(location %s) *" % (
                zip_tool, native_js_sources_from_apt),
        ]),
        tags=internal_tags,
        tools=["//third_party/zip:zip"],
        visibility=["//visibility:private"],
        local=True,
    )

    native_srcs_zips = native_srcs_zips + [native_js_sources_from_apt]

    # TODO: *Further* lock down 'visibility' of internal targets.
    if native_srcs:
      native_zip_name = base_name + "_native_bundle.zip"
      generate_zip(native_zip_name, native_srcs, "CONVENTION", testonly)
      native_srcs_zips += [":" + native_zip_name]

    # Do the transpilation
    js_sources_from_transpile = ":" + base_name + "_j2cl_transpile.js.zip"
    j2cl_transpile(
        name=base_name + "_j2cl_transpile",
        # Using -src.jar of the java_library since that includes APT generated src.
        srcs=["lib" + base_name + "_java_library-src.jar"],
        deps=java_deps,
        native_srcs_zips=native_srcs_zips,
        testonly=testonly,
        readable_source_maps=_readable_source_maps,
        declare_legacy_namespace=_declare_legacy_namespace,
        restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
        tags=internal_tags,
    )

    # Uh-oh: _js_import needs to depend on restricted_to=j2cl_compilation targets,
    # which it uses as classpath elements. But the _js_import can't itself be
    # restricted-to=j2cl_compilation, as it needs to be usable from "normal" build
    # rules. Our solution is to defeat the constraints system by building the
    # java_library as a host dep (by putting it in genrule.tools).
    #
    # Context:
    # https://groups.google.com/a/google.com/d/topic/target-constraints/ss38OI0UC9k/discussion
    # https://groups.google.com/a/google.com/d/topic/j2cl-team/IdWC-X-ky3s/discussion
    # https://docs.google.com/document/d/1bCVADLTenSVvVBkJ_Ip3EahdPdmEu1eyQ2wPGbE8sgM/edit?disco=AAAAAfa3IZU
    #
    # TODO(cpovirk): Find a less evil solution, maybe based on http://b/27044764

    # Copy the js to an unrestricted environment.

    transpiled_jszip_name = base_name + "_transpiled.js.zip"
    _do_env_copy(js_sources_from_transpile, transpiled_jszip_name, testonly)
    src_zips += [":" + transpiled_jszip_name]

    apt_jszip_name = base_name + "_apt.js.zip"
    _do_env_copy(js_sources_from_apt, apt_jszip_name, testonly)
    src_zips += [":" + apt_jszip_name]

    # Expose java sources similar to java_library
    java_src_jar = "lib" + base_name + "-src.jar"
    _do_env_copy("lib" + base_name + "_java_library-src.jar", java_src_jar, testonly)

  if _js_srcs:
    handrolled_js = base_name + "_handrolled.js.zip";
    generate_zip(handrolled_js, _js_srcs, "RELATIVE")
    src_zips += [":" + handrolled_js]

  merged_zip = base_name + ".js.zip"

  # Merge the zip as single source zip
  _merge_zips(
      srczips=src_zips,
      outzip=merged_zip,
      tags=internal_tags,
      testonly=testonly,
  )

  # Bring zip srcs into the js_library tree
  js_import(
      name=base_name + "_js_import",
      deps=js_deps,
      deps_mgmt=js_deps_mgmt,
      exports=js_exports,
      srczip=merged_zip if src_zips else None,
      # Direct automated dep picking tools away from this target.
      tags=internal_tags,
      testonly=testonly,
  )

  # This forces execution of j2cl_transpile() targets (both immediate and in the
  # dependency chain) when build has been invoked on the js_library() target.
  # Additionally, this is used as a workaround to make sure the zip ends up in the runfiles
  # directory as described in b/35847804.
  js_library_data = collections.uniq([merged_zip] + js_deps + js_exports)

  # Theoretically we should be able to just create the js_import() target, but
  # some Blaze rules (like jsunit_test()) require that all their direct deps be
  # exactly a js_library rule. They should allow anyone target that supplies a
  # JS provider, but they don't.
  native.js_library(
      name=base_name,
      exports=[base_name + "_js_import"],
      deps_mgmt = js_deps_mgmt,
      data=js_library_data,
      testonly=testonly,
      visibility=visibility,
      tags=tags,
  )

  if generate_build_test == None:
    generate_build_test = True

  if generate_build_test and src_zips:
    native.js_binary(
        name=base_name + "_js_binary",
        deps=[base_name],
        defs=J2CL_OPTIMIZED_DEFS,
        externs_list=_test_externs_list,
        include_default_externs="off" if _test_externs_list else "web",
        tags=internal_tags,
        compiler="//javascript/tools/jscompiler:head",
        testonly=1,
        visibility=["//visibility:private"],
    )

    build_test(
        name=base_name + "_build_test",
        targets=[
            base_name + "_js_binary",
            base_name + "_js_import",
        ],
        tags=internal_tags,
    )
