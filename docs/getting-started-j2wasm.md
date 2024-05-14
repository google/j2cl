
[//]: # TOC

# Getting Started for J2CL/Wasm (Experimental)

## Introduction

J2CL now has an experimental Wasm backend that transpiles Java to WebAssembly.
It is developed in collobration with Chrome team and based on Wasm GC spec that is
available in [Chrome 119+] (https://chromestatus.com/feature/6062715726462976).

Let's take a look at the simple hello world sample:

https://github.com/google/j2cl/tree/master/samples/wasm

## Building your first application

Tip: If you are not familiar with using Java with Bazel, it might be helpful to
start with the
[Bazel Java Tutorial](https://bazel.build/tutorials/java)
since J2CL closely follows Bazel Java idioms.

Let's get started with cloning the repository and build a sample app from
source:

-   Install [Bazelisk](https://github.com/bazelbuild/bazelisk):

```shell
    $ npm install -g @bazel/bazelisk
    $ alias bazel=bazelisk
```

-   Clone this repository:

```shell
      $ git clone https://github.com/google/j2cl.git
```

-   Build the wasm sample app:


```shell
      $ cd j2cl/samples/wasm
      $ bazel build src/main/java/com/google/j2cl/samples/wasm:jsapp
```

Let's run the development server to see our application in action:



-   Run the development server

```shell
      $ bazel run src/main/java/com/google/j2cl/samples/wasm:jsapp_dev_server
```
-   Navigate to 'http://localhost:6006/jsapp_dev.html" in your browser.

NOTE: J2CL currently uses experimental Wasm String functionality which might not be enabled
yet. If you see an
error message like: `Failed to load wasm: CompileError: WebAssembly.instantiateStreaming(): Unknown type code...`
, you can enable it via chrome://flags/#enable-experimental-webassembly-features

Please take the time to play with the code and get a better feeling of how
things are working.

## Live-reload

ibazel is file-system watcher that auto-triggers bazel build when needed. To use
it, just replace `bazel` with `ibazel` in any command:

-   Install [ibazel](https://github.com/bazelbuild/bazel-watcher#installation)
    and make sure it is in your path.

-   Run the wasm development server

```shell
      $ ibazel run src/main/java/com/google/j2cl/samples/wasm:jsapp_dev_server
```

-   Navigate to 'http://localhost:6006/helloworld_dev.html" in your browser.

-   Edit any source (e.g. `HelloWorld.java`), save and see the results. You will
    enjoy it more as it warms up!

You like build-on-save but you would like to refresh on your own terms? Pass
`-nolive_reload` while running ibazel.

## Unit testing

You can also write regular JUnit tests for your application and run them under
Wasm.

To run the sample unit test for our wasm sample app:

```shell
      $ bazel test src/test/java/com/google/j2cl/samples/wasm:HelloWorldTest
```

## Setting up your first project

To setup your first project you can use the sample as template:

```shell
      $ cp -R <j2cl-repo>/samples/wasm <my-repo>/<app-name>
      $ mv <my-repo>/<app-name>/WORKSPACE.remote <my-repo>/<app-name>/WORKSPACE
```

and you are done.

## Next steps

*   [J2CL/JS Getting Started](getting-started.md) to take look into J2CL's mature Java to JS solution which
    provides greater JavaScript interop and easier path for migrating existing apps.
*   [JsInterop Cookbook](jsinterop-by-example.md) for examples on how to interop
    with Java and JavaScript.
*   [J2CL Test](junit.md) for writing JavaScript transpiled JUnit tests.