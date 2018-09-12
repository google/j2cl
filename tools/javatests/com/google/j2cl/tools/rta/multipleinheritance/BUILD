# Apache2
licenses(["notice"])

load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

rta_test(
    name = "multipleinheritance",
    targets = [":multipleinheritance_library"],
    unused_members_golden_file = "unused_members.txt",
    unused_types_golden_file = "unused_types.txt",
)
