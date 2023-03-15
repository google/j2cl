load("//build_defs:rules.bzl", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

[
    j2cl_test(
        name = "TestShard%s" % i,
        srcs = [
            "TestCases%s.srcjar" % i,
            "TestShard%s.java" % i,
        ],
        javacopts = ["-nowarn"],
        deps = [
            "//third_party:gwt-jsinterop-annotations-j2cl",
            "//third_party:junit-j2cl",
        ],
    )
    for i in range(1, 51)
]
