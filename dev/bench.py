# Copyright 2021 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Check the impact of a local change on a benchmark."""

import subprocess
import sys

import repo_util


def main(argv):
  bench_names = [] if argv.bench_names == ["all"] else argv.bench_names

  if subprocess.call("v8 -e ''", shell=True):
    print("Make sure d8 is installed via jsvu")
    sys.exit(1)

  bench_map = _get_bench_map()
  bench_names = bench_names or bench_map.keys()
  # Benchs as list of (name1, {j2cl: target1, j2wasm: target1}) pairs
  benchs = [
      (n, repo_util.get_benchmarks(bench_map[n] + "_local", argv.platforms))
      for n in bench_names
  ]

  print("Building...")
  # Join all the targets to build them in one shot.
  targets = sum([list(bench.values()) for (_, bench) in benchs], [])
  repo_util.build(targets)

  multi_platform = len(argv.platforms) > 1
  if multi_platform:
    print("Starting benchmarks.")
  else:
    print(f"Starting benchmarks for {argv.platforms[0]}.")

  for name, bench_set in benchs:
    if multi_platform:
      print(f"[{name}]")
    else:
      print(name, end=": ", flush=True)

    for platform, target in bench_set.items():
      if multi_platform:
        print(platform, end=": ", flush=True)
      print(repo_util.run_cmd([repo_util.BIN_DIR + target], shell=True), end="")


_JRE_BENCHMARK_LIST_FILE = "benchmarking/java/com/google/j2cl/benchmarks/jre/benchmark_list.txt"
_OCTANE_BENCHMARK_LIST_FILE = "benchmarking/java/com/google/j2cl/benchmarks/octane/benchmark_list.txt"


def _get_bench_map():
  repo_util.build([_JRE_BENCHMARK_LIST_FILE, _OCTANE_BENCHMARK_LIST_FILE])

  bench_names = {}
  for bench_name in _read_gen_file(_JRE_BENCHMARK_LIST_FILE):
    bench_names[bench_name] = "jre/" + bench_name
  for bench_name in _read_gen_file(_OCTANE_BENCHMARK_LIST_FILE):
    bench_names[bench_name] = "octane/" + bench_name

  return bench_names


def _read_gen_file(file_path):
  with open(repo_util.BIN_DIR + file_path, "rt") as my_file:
    return my_file.read().splitlines()


def add_arguments(parser):
  parser.add_argument(
      "bench_names", nargs="+", metavar="<name>", help="Benchmark names")
