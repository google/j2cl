# Contained are ABI examples.

They document output readability and help to make sure that we do not generate
Closure compiler warnings.

Tests used for verification of output JS behavior and tests of output JS
readability are being kept separate.

These are the readability tests and they should not be executed as JS
because the line between these two styles of tests should not be
muddied. But when writing and reviewing do check build.log changes to make
sure that we do not generate Closure compiler warnings. If a new warning is
occurring and it is our mistake, fix it, otherwise file a bug with Closure.

Example input Java and output JS source in this folder and its subfolders
are copyright and licensed as follows:
/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

## Designs to be re-examined
- Normalize compound assignment, increment and decrement operations for long and
  boxed objects. Refer to previous dicussion at cl/101709039 and cl/98361767.
- Use annotation @abstract for abstract methods. (b/24539710).
- Use one file to export stubs for JS externs, or use separate files for each.
  (see previous discussion at cl/108187399)

## Tasks
- Add a Closure pass to get back the 2% regression introduced in cl/104162467.
- Investigate if label names (from LabeledStatements) are allowed to collide
    with other identifiers.
- Investigate if the size regression by cl/103334620 can be fixed.
    The direct cause of the size regression is that making
    Byte/Character/Float/Integer/Long/Short implement Comparable,
    and each Comparable.$markImplementor(...) was not stripped, even though
    they are not called. See cl/103587701 for a repro case and a deeper
    investigation of the cause.
- fix TODO in JavaScriptGeneratorStage.
- Investigate the performance of divide by zero checks specifically on integers
    in crypto libraries.  Add a @define if performance is significantly
    affected to turn off checks.
- hashcode test in 'numberobjectcalls' fails for long
- Investigate if Object.java, Enum.java and Class.java can be shared with GWT
- Private static fields should have private getters and setters.
- Import global native js types when JSCompiler supports goog.require() externs.
- Maybe further specialize Object equality() to avoid the Equality.$same() call
    if neither side could ever cast to String/Double/Boolean.
- Fix two errors with checktypes in "jsinteroptests".
  1. property m_getClass not defined on a native js types.
  2. constructors for subclasses of native js types.
- Investigate 45% leakage that was recovered in cl/107609415 and regressed in
    cl/111866974. A bunch of examples start leaking about 10kb of collections
    classes (TreeMap related) for unknown reasons. Need to know why JSCompiler
    thinks these things are live.
- Fix and double check equals/hashcode methods on all TypeDescriptor flavors.
  see cl/112695016 for more information.
- Generate more specific type annotation (function(): with specific parameter
  types and return type) for JsFunction types.

##JsInterop Restrictions to check
- JsConstructor:
  1). A class can have at most one JsConstructor.
  2). Other constructors must delegate to the JsConstructor.
  3). The JsConstructor must call super() to invoke 'JsConstructor' in Parent.
  4). 1) - 3) also apply on successors of JsConstructor class.
- JsMethod:
  1). namespace can only be defined on native static methods. (cl/106852320)
- JsFunction implementor
  1). members in JsFunction implementor cannot be JsProperty/JsMethod. (maybe)

## Tracking Closure warning issues
- https://b.corp.google.com/hotlists/269212
- b/20102666 WARNING - actual parameter of Foo.$markImplementor does not match
  formal parameter

## Feature Complete Java 7 TODOs
- packageclasscollision
- stringconstructor
- Enum.valueOf(Class<>, String)

## JsInterop TODOs
- catchjsexception
- jsconstructor
- jspackageinfo
- jstype (with deep namespace)
- jstype (cascade to inner classes)
- jstypeenum
- objectmethodsofnativetype
- subclassjsnativetype (with constructor)

## Feature Complete Java 8 TODOs
- interfacedefaultmethod
- methodreferences

## Polish TODOs
- anonymousclassforfunctionalinterface (transpiled to function)
- enumoptimization
- finalfield (readable JsDoc and no static setter)
- lightjsfunction (transpile lambda and anonymousclass to function)
- localvariablejsdoc
- private static clinit optimization
- shorterfieldmangling (for fields in final classes, like enums)
