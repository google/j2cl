# Description:
#  Custom browser definitions for Wasm.

load("@rules_webtesting//web:web.bzl", "custom_browser")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
)

# browser configuration enabling wasm features needed for j2wasm.
custom_browser(
    name = "chrome-wasm-linux",
    browser = "@rules_webtesting//browsers:chromium-local",
    metadata = "chrome_linux_enable_wasm_experiments.json",
)

custom_browser(
    name = "chrome-wasm-dev-linux",
    browser = "@rules_webtesting//browsers:chromium-local",
    metadata = "chrome_linux_enable_wasm_experiments.json",
)
