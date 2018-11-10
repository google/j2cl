#
# Copyright 2018 J2CL authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Macro to use for loading the J2CL repository"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")

def setup_j2cl_workspace():
    """Load all dependencies needed for J2CL."""

    native.maven_jar(
        name = "com_google_auto_common",
        artifact = "com.google.auto:auto-common:0.9",
    )
    native.maven_jar(
        name = "com_google_auto_service",
        artifact = "com.google.auto.service:auto-service:1.0-rc2",
    )

    native.maven_jar(
        name = "com_google_jsinterop_annotations",
        artifact = "com.google.jsinterop:jsinterop-annotations:HEAD-SNAPSHOT",
        repository = "https://oss.sonatype.org/content/repositories/google-snapshots/",
    )

    native.maven_jar(
        name = "org_apache_commons_collections",
        artifact = "commons-collections:commons-collections:3.2.2",
    )

    native.maven_jar(
        name = "org_apache_commons_lang2",
        artifact = "commons-lang:commons-lang:2.6",
    )

    native.maven_jar(
        name = "org_apache_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.6",
    )

    native.maven_jar(
        name = "org_apache_commons_text",
        artifact = "org.apache.commons:commons-text:1.2",
    )

    native.maven_jar(
        name = "org_apache_velocity",
        artifact = "org.apache.velocity:velocity:1.7",
    )

    native.maven_jar(
        name = "org_junit",
        artifact = "junit:junit:4.12",
    )

    native.maven_jar(
        name = "com_google_testing_compile",
        artifact = "com.google.testing.compile:compile-testing:0.15",
    )

    native.maven_jar(
        name = "org_mockito",
        artifact = "org.mockito:mockito-all:1.9.5",
    )

    native.maven_jar(
        name = "com_google_truth",
        artifact = "com.google.truth:truth:0.39",
    )

    # Eclipse JARs listed at
    # http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/

    http_jar(
        name = "org_eclipse_jdt_content_type",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.core.contenttype_3.7.0.v20180426-1644.jar",
        sha256 = "12198fd267ec8b57d1cc62975989cafa165da8bf52f29f0b00986e6960c2b439",
    )

    http_jar(
        name = "org_eclipse_jdt_jobs",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.core.jobs_3.10.0.v20180427-1454.jar",
        sha256 = "1de66682fb5aa56b075702ed4eb9957a0b9c0e16ef4533d02134571334386402",
    )

    http_jar(
        name = "org_eclipse_jdt_resources",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.core.resources_3.13.0.v20180512-1138.jar",
        sha256 = "61c7486ef1f01419ebb8fba80cdd2e813737b1d0a8cbe6ff841bac45af8fdf88",
    )

    http_jar(
        name = "org_eclipse_jdt_runtime",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.core.runtime_3.14.0.v20180417-0825.jar",
        sha256 = "323a203cbcc93631b55e7d7640ef68859059071916b19aa38d0b6e32fd7f4b35",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_common",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.equinox.common_3.10.0.v20180412-1130.jar",
        sha256 = "ed860f4559b45db15db7a8e42e215937602548b064f558af4ad02aa0701a0b02",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_preferences",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.equinox.preferences_3.7.100.v20180510-1129.jar",
        sha256 = "bff51f72029decb94ca6ee7205f6f162973db7693fa7b8df574fa47755047476",
    )

    http_jar(
        name = "org_eclipse_jdt_compiler_apt",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.jdt.compiler.apt_1.3.200.v20180523-0418.jar",
        sha256 = "02943e932afaf03322c76a33df53daad57365ddaf3b1a4539f8fbbb66b40cd42",
    )

    http_jar(
        name = "org_eclipse_jdt_core",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.jdt.core_3.14.0.v20180528-0519.jar",
        sha256 = "07a8bc95871e5cc82af1666577fb17a609b4d43692ecfd4e00a239cf16df9f03",
    )

    http_jar(
        name = "org_eclipse_jdt_osgi",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.osgi_3.13.0.v20180409-1500.jar",
        sha256 = "669995ebabc5c4128074a6589d722de2bce90cd66042320f60f0211ed3398ea2",
    )

    http_jar(
        name = "org_eclipse_jdt_text",
        url = "http://download.eclipse.org/eclipse/updates/4.8/R-4.8-201806110500/plugins/org.eclipse.text_3.6.300.v20180430-1330.jar",
        sha256 = "55f4470b98a7cad8aa22404afed4a03581478a29bb83e68b24d6a5a9e8508c61",
    )

    http_archive(
        name = "org_gwtproject_gwt",
        url = "https://gwt.googlesource.com/gwt/+archive/master.tar.gz",
    )

    http_archive(
        name = "io_bazel_rules_closure",
        strip_prefix = "rules_closure-master",
        url = "https://github.com/bazelbuild/rules_closure/archive/master.zip",
    )

    # proto_library and java_proto_library rules implicitly depend on
    # @com_google_protobuf for protoc and proto runtimes.
    http_archive(
        name = "com_google_protobuf",
        strip_prefix = "protobuf-3.6.1",
        urls = ["https://github.com/google/protobuf/archive/v3.6.1.zip"],
    )

    # needed for protobuf
    native.bind(
        name = "guava",
        actual = "@com_google_guava",
    )

    # needed for protobuf
    native.bind(
        name = "gson",
        actual = "@com_google_code_gson",
    )
