"""Macro for generating binary targets for j2cl apps."""

load(":j2cl_js_common.bzl", "J2CL_OPTIMIZED_DEFS", "js_binary", "js_devserver")

def j2cl_application(
        name,
        entry_points,
        deps,
        rewrite_polyfills = False,
        jre_logging_log_level = "OFF",
        jre_checks_check_level = "NORMAL",
        jre_class_metadata = "STRIPPED",
        closure_defines = dict(),
        extra_dev_resources = [],
        extra_production_args = [],
        dev_server_host = None,
        dev_server_port = None,
        **kwargs):
    """Create a J2CL application target.

    This generates couple of convenient pre-configured targets:
      <name>: closure_js_binary target that could be used for production.
      <name>_dev: closure_js_binary target that could be used for development.
      <name>_dev_server: a simple development server for simple apps.

    The devserver could be provided by extra resources. A custom host page can
    also be supplied as a resource where the development resources could be
    loaded by <link rel="import" href="<your_target_dev.html">.

    If you need to setup a custom development server, you can load
    <name>_dev_config.js and <name>_dev.js in your host page (in that order).
    However please note that <name>_dev_config.js is not a documented output and
    may get renamed or disappear in future releases.

    TODO(goktug): Document what needs to be done by apps with custom development
    server.

    Note: This macro is pretty bare bones right now but please let us know what
    kind of customizations you may need so we can look into incorparating them!

    Args:
        name: name of the rule.
        entry_points: JavaScript namespace of the entry point for the app.
        deps: dependencies of the app (e.g. j2cl_library, js_library, etc).
        rewrite_polyfills: rewrite ES6 library calls to use polyfills provided
          by the compiler's runtime to support "old" browsers. Only affects
          production binary.
        jre_logging_log_level: the minimum log level that java.util.logging
          should capture for production; the rest is optimized away.
        jre_checks_check_level: the level of checks provided by Java standard
          library emulation in production; the rest is optimized away.
          Note that development binary will still do checks but will throw
          assertion errors instead to prevent user code to incorrectly rely on
          specific exceptions to be thrown.
        jre_class_metadata: the level of class metadata emulated at the runtime.
          SIMPLE will provide basic metadata like class names, type of class etc.
          STRIPPED will remove most of the metadata provided at SIMPLE level to
          reduce the code size in production.
          Note that development binary will still provide some metadata to help
          with debugging.
        closure_defines: override the value of a variable defined by goog.define.
        extra_dev_resources: extra resource to serve for development server.
        extra_production_args: extra 'args' to pass to production binary.
        dev_server_host: override the dev server host
        dev_server_port: override the dev server port
        **kwargs: passed to underlying compilation and dev server.
    """

    entry_point_defs = ["--entry_point=goog:%s" % e for e in entry_points]

    #### Production binary setup ####

    define_prod = {
        "jre.checks.checkLevel": jre_checks_check_level,
        "jre.logging.logLevel": jre_logging_log_level,
        "jre.classMetadata": jre_class_metadata,
    }
    define_prod.update(closure_defines)
    define_prod_defs = ["--define=%s=%s" % (k, v) for (k, v) in define_prod.items()]

    config_name = "%s_readable_output_config" % name
    native.config_setting(
        name = config_name,
        values = {
            "define": "J2CL_APP_STYLE=PRETTY",
        },
    )
    extra_production_args = select({
        ":" + config_name: extra_production_args + [
            "--variable_renaming=OFF",
            "--property_renaming=OFF",
            "--pretty_print",
        ],
        "//conditions:default": extra_production_args,
    })

    js_binary(
        name = name,
        defs = J2CL_OPTIMIZED_DEFS + entry_point_defs + define_prod_defs + [
            "--rewrite_polyfills=%s" % rewrite_polyfills,
        ] + extra_production_args,
        deps = deps,
        **kwargs
    )

    #### Development binary setup ####

    # Note that unlike production, we don't include the generated config js in
    # the binary since JsCompiler re-orders it when there is an entry point.
    # It is essential for config js to be loaded the first to be effective for
    # uncompiled code. As a workaround we load it via script tag just before
    # dev.js (see below).
    define_dev = {
        # checks are  always enabled in debug but setting it make sure user code
        # doesn't accidentally rely on exceptions to be thrown by converting them
        # to assertion errors.
        "jre.checks.checkLevel": jre_checks_check_level,
    }
    define_dev.update(closure_defines)
    define_dev_content = "var CLOSURE_DEFINES = %s;" % json.encode(struct(**define_dev))

    index_html = """
<head><script>
const appName = "%s";
function loadScript(url) {
  const scriptElement = document.createElement("script");
  scriptElement.setAttribute("src", url);
  document.head.appendChild(scriptElement);
}
loadScript(`$${location.protocol}//$${location.hostname}:35729/livereload.js`);
loadScript(`$${appName}_dev_config.js`);
loadScript(`$${appName}_dev.js`);
</script></head>
"""

    dev_resources = [
        ":%s_dev.js" % name,
        _generate_file("%s_dev_config.js" % name, define_dev_content),
        _generate_file("%s_dev.html" % name, index_html % name),
    ] + extra_dev_resources

    js_devserver(
        name = "%s_dev" % name,
        entry_point_defs = entry_point_defs,
        deps = deps,
        dev_resources = dev_resources,
        dev_server_host = dev_server_host,
        dev_server_port = dev_server_port,
        **kwargs
    )

def _generate_file(file_name, content):
    native.genrule(
        name = file_name.replace(".", "_"),
        outs = [file_name],
        cmd = "echo '%s' > $@" % content,
    )
    return ":%s" % file_name
