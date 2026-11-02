load(":test_compilation.bzl", "test_compilation_suite")

config_setting(
    name = "optimized",
    values = {"compilation_mode": "opt"},
)

test_compilation_suite(name = "test_compilation_tests")
