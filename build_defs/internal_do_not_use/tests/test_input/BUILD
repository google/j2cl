load("@rules_shell//shell:sh_test.bzl", "sh_test")
load("//build_defs/internal_do_not_use:generate_test_input.bzl", "generate_test_input")

generated = generate_test_input(
    name = "nested_test",
    test_class = "example.Outer$Inner",
)

sh_test(
    name = "test_input_test",
    srcs = ["test_input_test.sh"],
    args = ["$(rootpath :%s)" % generated],
    data = [generated],
)
