"""Tests for package_sources.bzl."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load(":package_sources.bzl", "archive_relative_path")

def package_sources_test_suite(name):
    unittest.suite(
        name,
        _archive_relative_path_test,
    )

def _archive_relative_path_test_impl(ctx):
    env = unittest.begin(ctx)

    # In-repo paths have no */jre/java/ prefix, so the entry name is unchanged.
    asserts.equals(
        env,
        "jre/java/java/io/BufferedWriter.java",
        archive_relative_path("jre/java/java/io/BufferedWriter.java"),
    )
    asserts.equals(
        env,
        "jre/java/javasynth/java/lang/invoke/LambdaMetafactory.java",
        archive_relative_path("jre/java/javasynth/java/lang/invoke/LambdaMetafactory.java"),
    )
    asserts.equals(
        env,
        "jre/java/module-info.java",
        archive_relative_path("jre/java/module-info.java"),
    )
    asserts.equals(
        env,
        "jre/java/super-wasm/java/io/Serializable.java",
        archive_relative_path("jre/java/super-wasm/java/io/Serializable.java"),
    )

    # External and generated roots match */jre/java/ and are stripped.
    asserts.equals(
        env,
        "java/io/BufferedWriter.java",
        archive_relative_path("external/com_google_j2cl/jre/java/java/io/BufferedWriter.java"),
    )
    asserts.equals(
        env,
        "java/lang/invoke/LambdaMetafactory.java",
        archive_relative_path("external/com_google_j2cl/jre/java/javasynth/java/lang/invoke/LambdaMetafactory.java"),
    )
    asserts.equals(
        env,
        "super-wasm/java/io/Serializable.java",
        archive_relative_path("external/com_google_j2cl/jre/java/super-wasm/java/io/Serializable.java"),
    )
    asserts.equals(
        env,
        "module-info.java",
        archive_relative_path("external/com_google_j2cl/jre/java/module-info.java"),
    )
    asserts.equals(
        env,
        "super-wasm/javaemul/internal/wasmarray/WasmArray.java",
        archive_relative_path("bazel-out/darwin_arm64-fastbuild/bin/jre/java/super-wasm/javaemul/internal/wasmarray/WasmArray.java"),
    )

    # No jre/java segment is left unchanged. javasynth/ is stripped only after
    # the */jre/java/ deletion produced that leading prefix.
    asserts.equals(
        env,
        "some/other/Foo.java",
        archive_relative_path("some/other/Foo.java"),
    )
    asserts.equals(
        env,
        "javasynth/java/lang/invoke/LambdaMetafactory.java",
        archive_relative_path("javasynth/java/lang/invoke/LambdaMetafactory.java"),
    )

    return unittest.end(env)

_archive_relative_path_test = unittest.make(_archive_relative_path_test_impl)
