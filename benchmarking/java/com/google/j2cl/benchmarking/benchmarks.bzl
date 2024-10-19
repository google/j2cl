"""Utility functions for running benchmarks."""

load("@rules_java//java:defs.bzl", "java_binary")
load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmark_library.bzl", "benchmark_library")
load(
    "//:benchmarking/java/com/google/j2cl/benchmarking/templates.bzl",
    "create_j2cl_glue",
    "create_j2wasm_glue",
    "create_launcher",
)
load("//build_defs:rules.bzl", "j2cl_application", "j2wasm_application")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

_BENCHMARK_LIST_RULE_NAME = "benchmark_list"

def benchmark(name, deps = []):
    """Defines a benchmark that can be run on the jvw, web and wasm.

    Args:
      name: the name of the benchmark, also used as its primary file.
      deps: An optional list of dependencies.
    """

    # benchmark macro should be called before the call of gen_benchmark_list macro
    if native.existing_rule(_BENCHMARK_LIST_RULE_NAME):
        fail("all benchmark macro must be invoked before benchmark_list macro. Please check your BUILD file.")

    deps = deps + [
        "//benchmarking/java/com/google/j2cl/benchmarking/framework",
        "//:jsinterop-annotations",
    ]

    benchmark_java_package = get_java_package(native.package_name())

    benchmark_library(
        name = "%s_lib" % name,
        srcs = [
            "%s.java" % name,
            create_launcher(name, benchmark_java_package),
        ],
        deps = deps,
    )

    # JVM Benchmark
    native.alias(
        name = name,
        actual = "%s_local" % name,
    )
    java_binary(
        name = "%s_local" % name,
        runtime_deps = [":%s_lib" % name],
        main_class = "%s.%sLauncher" % (benchmark_java_package, name),
    )

    # J2CL benchmark
    closure_js_library(
        name = "%s-j2cl_glue" % name,
        srcs = [create_j2cl_glue(name, benchmark_java_package)],
        deps = [
            ":%s_lib-j2cl" % name,
        ],
    )

    j2cl_application(
        name = "%s_j2cl_entry" % name,
        deps = [":%s-j2cl_glue" % name],
        jre_checks_check_level = "MINIMAL",
        entry_points = ["%s_launcher" % name],
    )

    _d8_benchmark(
        name = "%s_local-j2cl" % name,
        data = [":%s_j2cl_entry.js" % name],
        tags = ["j2cl"],
    )

    # J2WASM Benchmark
    j2wasm_application(
        name = "%s_j2wasm_binary" % name,
        deps = [":%s_lib-j2wasm" % name],
        entry_points = [
            "%s.%sLauncher#execute" % (benchmark_java_package, name),
            "%s.%sLauncher#prepareForRunOnce" % (benchmark_java_package, name),
            "%s.%sLauncher#runOnce" % (benchmark_java_package, name),
        ],
    )

    wasm_url = "%s_j2wasm_binary.wasm" % name
    wasm_module_name = "%s_j2wasm_binary.j2wasm" % name
    closure_js_library(
        name = "%s_j2wasm_glue" % name,
        srcs = [create_j2wasm_glue(name, wasm_url, wasm_module_name)],
        lenient = True,
        deps = [
            ":%s_j2wasm_binary" % name,
        ],
    )
    j2cl_application(
        name = "%s_j2wasm_entry" % name,
        deps = [":%s_j2wasm_glue" % name],
        entry_points = ["%s_launcher" % name],
    )

    _d8_benchmark(
        name = "%s_local-j2wasm" % name,
        data = [
            ":%s_j2wasm_entry.js" % name,
            ":%s_j2wasm_binary.wasm" % name,
        ],
        tags = ["j2wasm"],
    )

def gen_benchmark_suite(name):

    all_benchmarks = [b for b in native.existing_rules().keys() if b.endswith("Benchmark")]

    native.genrule(
        name = _BENCHMARK_LIST_RULE_NAME,
        outs = [_BENCHMARK_LIST_RULE_NAME + ".txt"],
        cmd = "cat > $@ <<END\n%s\nEND\n" % "\n".join(all_benchmarks),
        output_to_bindir = 1,
        visibility = [
            "//benchmarking/java/com/google/j2cl/benchmarking/perfgate:__pkg__",
            "//apps/testing/performance/perforate/configuration/generators:__pkg__",
            "//configs/devtools/hawkeye/workspace/j2cl:__subpackages__",
        ],
    )

def _d8_benchmark(name, data, tags):
    native.genrule(
        name = "gen_%s_sh" % name,
        cmd = "echo cd $$(dirname $(location %s)) '&&' " % data[0] +
              "v8 --expose-gc --experimental-wasm-imported-strings --turboshaft-future" +
              " $$(basename $(location %s)) > $@" % data[0] +
              " -e \\''const results = JSON.parse(execute())'\\'" +
              " -e \\''console.log(results.reduce((a, b) => a + b) / results.length)'\\' ",
        outs = ["%s.sh" % name],
        srcs = data,
        tags = tags,
    )

    native.sh_binary(
        name = name,
        srcs = ["gen_%s_sh" % name],
        data = data,
        tags = tags,
    )
