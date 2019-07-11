"""Macro for generating binary targets for j2cl apps."""

load(":j2cl_js_common.bzl", "J2CL_OPTIMIZED_DEFS", "js_binary", "js_devserver", "simple_js_lib")

def j2cl_application(
        name,
        entry_points,
        deps,
        extra_defs = [],
        formatting = None,
        rewrite_polyfills = False,
        language = None,
        jre_logging_log_level = "OFF",
        jre_checks_check_level = "NORMAL",
        closure_defines = dict(),
        extra_dev_resources = [],
        **kwargs):
    """Create a J2CL application target.

    This generates couple of convenient pre-configured targets:
      <name>: closure_js_binary target that could be used for production.
      <name>_dev: closure_js_binary target that could be used for development.
      <name>_dev_server: a simple development server for simple apps.

    The devserver could be provided by extra resources. A custom host page can
    also be supplied as a resource where the development resources could be
    loaded by <link rel="import" href="<your_target_dev.html">.
    TODO(goktug): Document what needs to be done by apps with custom development
    server.

    Note: This macro is pretty bare bones right now but please let us know what
    kind of customizations you may need so we can look into incorparating them!

    Args:
        name: name of the rule.
        entry_points: JavaScript namespace of the entry point for the app.
        deps: dependencies of the app (e.g. j2cl_library, js_library, etc).
        extra_defs: Extra flags to pass to the Closure Compiler.
        formatting: Whether to pretty-print or minify the code.
        rewrite_polyfills: rewrite ES6 library calls to use polyfills provided
          by the compiler's runtime to support "old" browsers. Only affects
          production binary.
        language: Output target language to pass to Closure Compiler. If no
          value is passed, the default is used (`ECMASCRIPT5` at the time of
          this writing).
        jre_logging_log_level: the minimum log level that java.util.logging
            should capture for production; the rest is optimized away.
        jre_checks_check_level: the level of checks provided by Java standard
          library emulation in production; the rest is optimized away.
          Note that development binary will still do checks but will throw
          assertion errors instead to prevent user code to incorrectly rely on
          specific exceptions to be thrown.
        closure_defines: override the value of a variable defined by goog.define.
        extra_dev_resources: extra resource to serve for development server.
        kwargs: Extra arguments to pass into `rules_closure`.
    """

    entry_point_defs = ["--entry_point=goog:%s" % e for e in entry_points]

    #### Production binary setup ####

    define_defaults = {
        "jre.checks.checkLevel": jre_checks_check_level,
        "jre.logging.logLevel": jre_logging_log_level,
    }
    _define_js("%s_config" % name, define_defaults, closure_defines)

    if language:
        kwargs["language"] = language
    if formatting:
        kwargs["formatting"] = formatting

    js_binary(
        name = name,
        defs = J2CL_OPTIMIZED_DEFS + entry_point_defs + [
            "--rewrite_polyfills=%s" % rewrite_polyfills,
        ] + extra_defs,
        deps = [":%s_config" % name] + deps,
        **kwargs
    )

    #### Development binary setup ####

    # Note that unlike production, we don't include the generated config js in
    # the binary since JsCompiler re-orders it when there is an entry point.
    # It is essential for config js to be loaded the first to be effective for
    # uncompiled code. As a workaround we load it via script tag just before
    # dev.js (see below).
    define_dev_defaults = {
        # closure debug loader is slow and complains about cyclic deps.
        "goog.ENABLE_DEBUG_LOADER": False,
        # checks are  always enabled in debug but setting it make sure user code
        # doesn't accidentally rely on exceptions to be thrown by converting them
        # to assertion errors.
        "jre.checks.checkLevel": jre_checks_check_level,
    }
    _define_js("%s_dev_config" % name, define_dev_defaults, closure_defines)

    index_html = "<script src='http://localhost:35729/livereload.js'></script>"
    index_html += "<script src='%s_dev_config.js'></script>" % name
    index_html += "<script src='%s_dev.js'></script>" % name
    dev_resources = [
        ":%s_dev.js" % name,
        ":%s_dev_config.js" % name,
        _generate_file("%s_dev.html" % name, index_html),
    ] + extra_dev_resources

    js_devserver(
        name = "%s_dev" % name,
        entry_point_defs = entry_point_defs,
        deps = deps,
        dev_resources = dev_resources,
        **kwargs
    )

def _define_js(name, defines, user_overrides):
    defines.update(user_overrides)
    content = "var CLOSURE_DEFINES = %s;" % struct(**defines).to_json()
    simple_js_lib(
        name = name,
        srcs = [_generate_file("%s.js" % name, content)],
    )

def _generate_file(file_name, content):
    native.genrule(
        name = file_name.replace(".", "_"),
        outs = [file_name],
        cmd = "echo '%s' > $@" % content,
    )
    return ":%s" % file_name
