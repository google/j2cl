"""Profiling utilities for J2CL."""

load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def add_profiling_support(ctx, mnemonic, outputs, args):
    """Adds profiling support by declaring a profiling output file if conditions are met.

    Args:
        ctx: The Starlark context.
        mnemonic: The mnemonic of the action being profiled.
        outputs: The list of outputs to append the declared profile file to.
        args: The command-line args target to add the profile output path argument to.
    """
    profiling_filter = ctx.attr._profiling_filter[BuildSettingInfo].value
    if profiling_filter and profiling_filter in str(ctx.label):
        profile_output = ctx.actions.declare_file(ctx.label.name + "_" + mnemonic + ".profile")
        outputs.append(profile_output)
        args.add("-profileOutput", profile_output)
        print("Profiling %s %s" % (ctx.label, mnemonic))  # buildifier: disable=print
        print("pprof --flame %s" % profile_output.path)  # buildifier: disable=print
