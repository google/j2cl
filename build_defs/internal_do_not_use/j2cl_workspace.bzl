"""Macro to use for loading the J2CL repository"""

load("@bazel_skylib//lib:versions.bzl", "versions")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")
load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")
load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

_MAVEN_CENTRAL_URLS = ["https://repo1.maven.org/maven2/"]

def setup_j2cl_workspace(**kwargs):
    """Load all dependencies needed for J2CL."""

    versions.check("5.1.0")  # The version J2CL currently have a CI setup for.

    rules_closure_dependencies(
        omit_com_google_auto_common = True,
        **kwargs
    )

    jvm_maven_import_external(
        name = "com_google_auto_common",
        artifact = "com.google.auto:auto-common:1.1.2",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "bfe85e517250fc208afd2b031a2ba80f26529c92536484841b4a60661ca1e3f5",
    )

    jvm_maven_import_external(
        name = "com_google_auto_service_annotations",
        artifact = "com.google.auto.service:auto-service-annotations:1.0-rc7",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "986dc826fa0a43bf9f04194c1a8667774f4f44190ec816b08554b47891ba5459",
    )

    jvm_maven_import_external(
        name = "com_google_auto_service",
        artifact = "com.google.auto.service:auto-service:1.0-rc7",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "24f13c98baf5fb87e7502fd87130457941cda405ec6ff28d8ff964a8f58a82ed",
    )

    # We cannot replace com_google_jsinterop_annotations so choose a different name
    http_archive(
        name = "com_google_jsinterop_annotations-j2cl",
        urls = ["https://github.com/google/jsinterop-annotations/archive/02ae698e12ae8dccb96d55ac771e87caf404104d.zip"],
        strip_prefix = "jsinterop-annotations-02ae698e12ae8dccb96d55ac771e87caf404104d",
        sha256 = "0bfb1eca6b47d779b40e0fdb6e6e1bdad5a2abfcef1d325448b2dee3a5ab6e47",
    )

    http_archive(
      name = "bazel_common_javadoc",
      strip_prefix = "bazel-common-213ecb74698948ce4e1597c9247f2ab8933553d7/tools/javadoc",
      urls = ["https://github.com/google/bazel-common/archive/213ecb74698948ce4e1597c9247f2ab8933553d7.zip"],
      sha256 = "dcefae7d8561f684f305223aad530e5dccb4e8068a653edfc2c89abe9570d2a6",
    )

    jvm_maven_import_external(
        name = "j2objc_annotations",
        artifact = "com.google.j2objc:j2objc-annotations:1.3",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_jspecify",
        artifact = "org.jspecify:jspecify:0.2.0",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_apache_commons_collections",
        artifact = "commons-collections:commons-collections:3.2.2",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "eeeae917917144a68a741d4c0dff66aa5c5c5fd85593ff217bced3fc8ca783b8",
    )

    jvm_maven_import_external(
        name = "org_apache_commons_lang2",
        artifact = "commons-lang:commons-lang:2.6",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "50f11b09f877c294d56f24463f47d28f929cf5044f648661c0f0cfbae9a2f49c",
    )

    jvm_maven_import_external(
        name = "com_google_escapevelocity",
        artifact = "com.google.escapevelocity:escapevelocity:1.1",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "37e76e4466836dedb864fb82355cd01c3bd21325ab642d89a0f759291b171231",
    )

    jvm_maven_import_external(
        name = "org_junit",
        artifact = "junit:junit:4.13.2",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_testing_compile",
        artifact = "com.google.testing.compile:compile-testing:0.15",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_mockito",
        artifact = "org.mockito:mockito-all:1.9.5",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_truth",
        artifact = "com.google.truth:truth:1.0",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    # TODO(b/135461024): for now J2CL uses a prepackaged version of javac. But in the future it
    # might be better to tie in to the Java platform in bazel and control the version there.
    jvm_maven_import_external(
        name = "com_sun_tools_javac",
        artifact = "com.google.errorprone:javac:9+181-r4173-1",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
        artifact_sha256 = "1d8d347a0e1579f3fc86ac04d1974e489afc66357f0009ac9804a7ac30912ed6",
    )

    # Eclipse JARs listed at
    # http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/

    http_jar(
        name = "org_eclipse_jdt_content_type",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.core.contenttype_3.7.700.v20200517-1644.jar",
        sha256 = "af418cced47512a7cad606ea9a1114267bc224387abcedd639bae8d3a7fb10b9",
    )

    http_jar(
        name = "org_eclipse_jdt_jobs",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.core.jobs_3.10.800.v20200421-0950.jar",
        sha256 = "4d0042425dcc3655c08654351c08b1645ccb309ab5de45743455bfce4849e917",
    )

    http_jar(
        name = "org_eclipse_jdt_resources",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.core.resources_3.13.700.v20200209-1624.jar",
        sha256 = "ce021447dbea30a4e5ddb3f52534cd2794fb52855071b8dcf257b936ab162168",
    )

    http_jar(
        name = "org_eclipse_jdt_runtime",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.core.runtime_3.18.0.v20200506-2143.jar",
        sha256 = "b5aebc31d480efff38f910a6eab791c2de7b126a47d260252e097b5a27bd0165",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_common",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.equinox.common_3.12.0.v20200504-1602.jar",
        sha256 = "761f9175b9d294d122c1aa92048688f0b71dd81e808c64cbb245ca7539950716",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_preferences",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.equinox.preferences_3.8.0.v20200422-1833.jar",
        sha256 = "ca62478a40cffdfe9a10dcfb9f8fada760a93644a7de2c2d1897235f67f57b42",
    )

    http_jar(
        name = "org_eclipse_jdt_compiler_apt",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.jdt.apt.core_3.6.600.v20200529-1546.jar",
        sha256 = "0559677c8d0528fbdfa3a82b4a16661894a9b64a342e418809c64945bb5d3ef1",
    )

    http_jar(
        name = "org_eclipse_jdt_core",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.jdt.core_3.22.0.v20200530-2032.jar",
        sha256 = "af89d348c24917506675767fc1534a0d673355d334fbfadd264b9e45ccd9c34c",
    )

    http_jar(
        name = "org_eclipse_jdt_osgi",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.osgi_3.15.300.v20200520-1959.jar",
        sha256 = "a3544cde6924babf8aff8323f7452ace232d01d040e20d9f9f43027d7b945424",
    )

    http_jar(
        name = "org_eclipse_jdt_text",
        url = "http://download.eclipse.org/eclipse/updates/4.16/R-4.16-202006040540/plugins/org.eclipse.text_3.10.200.v20200428-0633.jar",
        sha256 = "83ce07ec2058d8d629feb4e269216e286560b0e4587dea883f4e16b64ea51cad",
    )

    http_archive(
        name = "org_jbox2d",
        strip_prefix = "jbox2d-jbox2d-2.2.1.1/jbox2d-library",
        urls = ["https://github.com/jbox2d/jbox2d/archive/jbox2d-2.2.1.1.zip"],
        sha256 = "088e5fc0f56c75f82c289c4721d9faf46a309e258d3ee647799622ef82e60303",
        patches = ["@com_google_j2cl//transpiler/javatests/com/google/j2cl/integration/java/box2d:jbox2d.patch"],
        build_file_content = '''
load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_library")

package(default_visibility = ["//visibility:public"])

_JAVACOPTS = [
    "-Xep:EqualsHashCode:OFF",  # See go/equals-hashcode-lsc
]

java_library(
    name = "jbox2d",
    srcs = glob(["**/*.java"]),
    javacopts = _JAVACOPTS,
    deps = [
        "@com_google_j2cl//:jsinterop-annotations",
    ],
)
j2cl_library(
    name = "jbox2d-j2cl",
    srcs = glob(
        ["src/main/java/**/*.java"],
        exclude = [
            "**/StrictMath.java",
            "**/PlatformMathUtils.java",
            "**/Timer.java",
            "**/profile/**",
        ],
    ) + glob(["src/main/java/org/jbox2d/gwtemul/**/*.java"],
        exclude = ["**/StrictMath.java"],
    ),
    javacopts = _JAVACOPTS,
    deps = [
        "@com_google_j2cl//:jsinterop-annotations-j2cl",
    ],
)
''',
    )

    kotlin_repositories(
        compiler_release = {
            "urls": [
                "https://github.com/JetBrains/kotlin/releases/download/v1.6.10/kotlin-compiler-1.6.10.zip",
            ],
            "sha256": "432267996d0d6b4b17ca8de0f878e44d4a099b7e9f1587a98edc4d27e76c215a",
        },
    )
    kt_register_toolchains()

    # Required by protobuf_java_util
    native.bind(
        name = "guava",
        actual = "@com_google_guava",
    )

    # Required by protobuf_java_util
    native.bind(
        name = "gson",
        actual = "@com_google_code_gson",
    )

    # Required by protobuf_java_util
    native.bind(
        name = "error_prone_annotations",
        actual = "@com_google_errorprone_error_prone_annotations",
    )
