load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmarks.bzl", "benchmark", "gen_benchmark_suite")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

benchmark(
    name = "Box2dBenchmark",
    deps = ["//samples/box2d/src/main/java:box2d_library"],
)

benchmark(
    name = "RichardsBenchmark",
    deps = [
        "//benchmarking/java/com/google/j2cl/benchmarks/octane/richards",
        "//third_party:jsr305_annotations",
    ],
)

# TODO(b/190432701): uncomment when fixed
#benchmark(
#    name = "NavierStokesBenchmark",
#    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/octane/navierstokes"],
#)

benchmark(
    name = "RayTraceBenchmark",
    deps = [
        "//benchmarking/java/com/google/j2cl/benchmarks/octane/raytrace",
        "//third_party:jsr305_annotations",
    ],
)

benchmark(
    name = "DeltaBlueBenchmark",
    deps = [
        "//benchmarking/java/com/google/j2cl/benchmarks/octane/deltablue",
        "//third_party:jsr305_annotations",
    ],
)

gen_benchmark_suite(name = "octane")
