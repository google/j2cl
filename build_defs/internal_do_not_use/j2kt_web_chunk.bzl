"""Wraps a web_chunk rule, applying the J2KT web transition."""

load("@bazel_tools//tools/build_defs/js/providers:providers.bzl", "JsInfo", "JsModuleInfo")
load(":j2kt_web_transition.bzl", "j2kt_web_transition")

visibility("private")

def _j2kt_web_chunk_impl(ctx):
    # Note: even though the attr is define a single label, applying user-defined transition  causes
    #   it to become a list even though it'll only ever have one element.
    if len(ctx.attr.target) != 1:
        fail("target must have exactly one element")
    target = ctx.attr.target[0]
    return [
        target[JsInfo],
        target[JsModuleInfo],
        target[DefaultInfo],
        target[OutputGroupInfo],
    ]

j2kt_web_chunk = rule(
    implementation = _j2kt_web_chunk_impl,
    attrs = {
        "target": attr.label(cfg = j2kt_web_transition, providers = [JsInfo, JsModuleInfo]),
    },
    provides = [JsInfo, JsModuleInfo],
)
