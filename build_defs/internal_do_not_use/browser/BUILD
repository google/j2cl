# Description:
#  Custom browser definitions for Wasm.

load("@rules_closure//closure:defs.bzl", "custom_browser")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
)

# browser configuration enabling wasm features needed for j2wasm.
custom_browser(
    name = "chrome-linux",
    browser = "@rules_browsers//browsers/chromium:chromium",
    metadata = "chrome_linux_enable_wasm_experiments.json",
)
