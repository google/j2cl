load("//third_party/java/j2cl_proto:j2cl_proto_library.bzl", "j2cl_proto_library")
load(
    "//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl",
    "rta_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

proto_library(
    name = "foo",
    srcs = ["foo.proto"],
    deps = [
        "//java/com/google/protobuf/contrib/immutablejs:options_proto",
    ],
)

proto_library(
    name = "bar",
    srcs = ["bar.proto"],
    deps = [
        ":foo",
        "//java/com/google/protobuf/contrib/immutablejs:options_proto",
    ],
)

j2cl_proto_library(
    name = "bar_j2cl_proto",
    deps = [":bar"],
)

rta_test(
    name = "proto",
    targets = [":bar_j2cl_proto"],
    unused_types_golden_file = "unused_types.txt",
)
