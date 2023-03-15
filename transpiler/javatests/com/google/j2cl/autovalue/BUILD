load("//build_defs:rules.bzl", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_test(
    name = "AutoValueTest",
    srcs = ["AutoValueTest.java"],
    optimize_autovalue = True,
    deps = [
        "//third_party:auto_value-j2cl",
        "//third_party:guava-j2cl",
        "//third_party:guava_testing-j2cl",
        "//third_party:jsr305_annotations-j2cl",
        "//third_party:junit-j2cl",
        "//third_party:truth-j2cl",
    ],
)
