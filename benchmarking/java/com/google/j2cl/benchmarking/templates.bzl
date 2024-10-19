"""Set of templates for running benchmarks."""

_LAUNCHER_TEMPLATE = """
package #benchmark_package#;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import com.google.j2cl.benchmarking.framework.BenchmarkExecutor;
import com.google.j2cl.benchmarking.framework.BenchmarkResult;
import jsinterop.annotations.JsType;
import java.util.Arrays;

@JsType
public class #benchmarkName#Launcher {
  private static BenchmarkResult doExecute() {
    return BenchmarkExecutor.execute(new #benchmarkName#());
  }

  public static void main(String[] args) {
    System.out.println(doExecute().getAverageThroughput());
  }

  public static String execute() {
    return Arrays.toString(doExecute().getThroughputs());
  }

  private static AbstractBenchmark benchmark;

  public static void prepareForRunOnce() {
    benchmark = new #benchmarkName#();
    BenchmarkExecutor.prepareForRunOnce(benchmark);
  }

  public static void runOnce() {
    benchmark.run();
    benchmark = null; // Make sure it is only executed once.
  }
}
"""

# JS templates
_J2CL_GLUE_TEMPLATE = """
goog.module('#benchmarkName#_launcher')

const Launcher = goog.require('#benchmark_package#.#benchmarkName#Launcher');

goog.exportSymbol("execute", Launcher.execute);
goog.exportSymbol("prepareForRunOnce", Launcher.prepareForRunOnce);
goog.exportSymbol("runOnce", Launcher.runOnce);
"""

# Wasm templates
_J2WASM_GLUE_TEMPLATE = """
goog.module('#benchmarkName#_launcher')

const j2wasm = goog.require('#wasm_module_name#');

if (typeof read == 'undefined') {
  // Running on browser, fetch the file from server.
  j2wasm.instantiateStreaming("#wasm_url#")
      .then((instance) => Object.assign(goog.global, instance.exports));
} else {
  // Running in d8, read the file locally.
  const buffer = read('#wasm_url#', 'binary');
  // Add a simple TextDecoder polyfill. Will be removed soon with magic-string imports.
  goog.global['TextDecoder'] = class {
    /** @suppress {checkTypes} JSC_ILLEGAL_PROPERTY_ACCESS */
    ["decode"](buffer) {
      return String.fromCharCode.apply(null, new Uint8Array(buffer));
    }
  };
  // Add placeholders for atob and btoa. These are only available in browser, but are not used.
  goog.global['atob'] = (a) => {
    throw new Error('atob not supported');
  };
  goog.global['btoa'] = (b) => {
    throw new Error('btoa not supported');
  };
  Object.assign(goog.global, j2wasm.instantiateBlocking(buffer).exports);
}

"""

def _create_file_from_template(file_name, template, replacements):
    file_content = template
    for pattern in replacements:
        file_content = file_content.replace(pattern, replacements[pattern])

    native.genrule(
        name = file_name.replace(".", "_") + "_generate_file",
        outs = [file_name],
        cmd = "cat > $@ <<END\n%s\nEND\n" % file_content,
    )
    return file_name

def create_launcher(benchmark_name, benchmark_java_package):
    return _create_file_from_template(
        "%sLauncher.java" % benchmark_name,
        _LAUNCHER_TEMPLATE,
        {
            "#benchmarkName#": benchmark_name,
            "#benchmark_package#": benchmark_java_package,
        },
    )

def create_j2cl_glue(benchmark_name, benchmark_java_package):
    return _create_file_from_template(
        "%s_j2cl_launcher.js" % benchmark_name,
        _J2CL_GLUE_TEMPLATE,
        {
            "#benchmarkName#": benchmark_name,
            "#benchmark_package#": benchmark_java_package,
        },
    )

def create_j2wasm_glue(benchmark_name, wasm_url, wasm_module_name):
    return _create_file_from_template(
        "%s_j2wasm_launcher.js" % benchmark_name,
        _J2WASM_GLUE_TEMPLATE,
        {
            "#benchmarkName#": benchmark_name,
            "#wasm_url#": wasm_url,
            "#wasm_module_name#": wasm_module_name,
        },
    )
