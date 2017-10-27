# NOTE: This package uses internal tools. It should not be made open-source.

# Apache2
licenses(["notice"])

integration_package = "//transpiler/javatests/com/google/j2cl/transpiler/integration"

integration_test_names = [
    "alllongoperations",
    "assertsimple",
    "box2d_default",
    "box2d_noclassmetadata_checksmin",
    "casttoclass",
    "compiletimeconstant",
    "cyclicclinits",
    "emptyclass",
    "lambdas",
]

java_test(
    name = "J2clJavaScriptSizeTest",
    srcs = ["J2clJavaScriptSizeTest.java"],
    args = ["--nooutputredirect"],
    data = ["%s/%s:optimized_js" % (integration_package, f) for f in integration_test_names],
    shard_count = 1,
    # TODO(b/28286052): Put this test back on TAP.
    tags = ["notap"],
    deps = [
        "//java/com/google/ads/testing/testmetricsrecorder",
        "//java/com/google/common/collect",
        "//java/com/google/testing/javascript:size_helper",
        "//java/com/google/testing/util",
        "//third_party/java/junit",
    ],
)
