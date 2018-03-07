# J2cl Junit support

go/j2cl-junit

J2cl supports translating JUnit3 and JUnit4 tests to jsunit tests.

[TOC]

## Build rule j2cl_test

The [j2cl_test] (https://cs.corp.google.com/#piper///depot/google3/third_party/java/j2cl/j2cl_test.bzl)
build macro behaves very similar to java_test, but compiles Java to JavaScript and runs
the output as a jsunit_test. On top of accepting regular Java attributes
it also accepts all attributes of jsunit_test. These can be used to configure
things like the browser environment or JsCompiler parameters.

See a simple [JUnit4 test example]
(https://cs.corp.google.com/#piper///depot/google3/third_party/java_src/j2cl/samples/helloworld/javatests/com/google/j2cl/samples/helloworld/)
as well as a [JUnit3 test example]
(https://cs.corp.google.com/#piper///depot/google3/third_party/java_src/j2cl/samples/junit3/javatests/com/google/j2cl/samples/junit3/).


## Debugging j2cl_test

Since j2cl_test generates a jsunit_test, debugging a j2cl_test works like
debugging a jsunit_test.
To run your test locally add "_debug" to the name of your test:

```
blaze run your/project:testname_debug
```

This will print a url in the console like this:

```
-------------------------------------------------------------------
 Test file service started at http://yourmachine.corp.google.com:4000
-------------------------------------------------------------------
```

Open this url in any browser and click on any of the links to
the generated test suites.

For seeing code changes in a local browser while debugging simply run the target
with iblaze instead of blaze. You can refresh the browser once the blaze build
finishes:

```
iblaze run your/project:testname_debug
```

You can use http://google3/third_party/java/j2cl/blazerc to configure blaze for
quicker iterations. Also make sure to read [Debugging JsUnit
Tests](/testing/web/g3doc/reference/jsunit/debugging-tests.md)

## Async testing support

Asynchronous testing is required when interacting with asynchronous APIs on the
web. Contrary to testing in Java one can not use a thread and simply wait on
another task, since JavaScript is single threaded.

Instead j2cl_test follows Closure standard practices by using promise-like
objects ([IThenable](https://cs.corp.google.com/piper///depot/google3/javascript/externs/es6.js?l=1068&cl=138227582)).
Simply return a Thenable instance from your test method, that later resolves
test result. You will need to set a timeout on the test.


```
public class MyTest {
  @Test(timeout = 200L)
  public SomeThenable testResolvesAfterDelay() {
    ...
  }
}

```

The return type needs to be a structural Thenable:

 - It is a @JsType
 - It has a then method with either one or two parameters
 - All the parameter types of the then methods are annotated with @JsFunction


## Differences in JUnit emulation / features
J2CL provides its own emulation of JUnit (just like it provides its own
emulation of java.*). Since JUnit uses non compatible features in some areas
(e.g. reflection), J2CLs emulation is not 100% compatible. Most notable parts
are:

 * No support for JUnit3 test suites (use JUnit4 test suites instead)
 * Test need to be top level classes
 * No Support for JUnit4 test runners, most runner classes are missing in
   emulation

For more details see [local modifications](https://cs.corp.google.com/piper///depot/google3/third_party/java_src/j2cl/junit/opensource/local_modifications.txt). 

If you see other missing features please file a bug:
https://b.corp.google.com/issues/new?component=138607&template=462243
