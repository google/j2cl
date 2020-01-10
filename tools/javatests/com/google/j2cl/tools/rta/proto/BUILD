load(
    "//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl",
    "rta_test",
)
load("//java/com/google/protobuf/contrib/gwt:j2cl_proto.bzl", "j2cl_proto_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

proto_library(
    name = "foo",
    srcs = ["foo.proto"],
)

proto_library(
    name = "bar",
    srcs = ["bar.proto"],
    deps = [":foo"],
)

j2cl_proto_library(
    name = "bar_j2cl_proto",
    deps = [":bar"],
)

rta_test(
    name = "proto",
    targets = [":bar_j2cl_proto"],
    unused_members_golden_file = "unused_members.txt",
    unused_types_golden_file = "unused_types.txt",
)
