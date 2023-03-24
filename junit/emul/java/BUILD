load("//build_defs:rules.bzl", "j2cl_library", "j2kt_jvm_import", "j2kt_native_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],  # Eclipse Public License - v 1.0
)

j2cl_library(
    name = "junit_emul",
    srcs = glob(
        ["**/*.java"],
        exclude = [
            "org/junit/super-j2kt/*.java",
        ],
    ),
    javacopts = [
        "-Xep:DoNotCall:OFF",
    ],
    visibility = ["//:__pkg__"],
)

# For J2kt JVM test, we delegate back to the official JUnit because we cannot have both
# J2CL/J2KT version of JUnit and official JUnit.
j2kt_jvm_import(
    name = "junit_emul-j2kt-jvm",
    testonly = 1,
    jar = "//third_party:junit",
    visibility = ["//:__pkg__"],
)

j2kt_native_library(
    name = "junit_emul-j2kt-native",
    srcs = glob(
        ["**/*.java"],
        exclude = [
            "org/junit/Assert.java",
        ],
    ),
    javacopts = [
        "-Xep:DoNotCall:OFF",
    ],
    visibility = ["//:__pkg__"],
)
