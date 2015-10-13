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

## Tasks
- Add a Closure pass to get back the 2% regression introduced in cl/104162467.
- Fix "simpleautoboxing" build.log, we're currently omitting Double->double and Boolean->boolean
    boxing operations and it's resulting in Closure seeing us try to collapse ?number->number and
    ?boolean->boolean.
- Fix "inconsistent return type" error with checktypes in "LambdaNestingInAnonymousClasses"
- Investigate if label names (from LabeledStatements) are allowed to collide with other identifiers.
- Investigate if the size regression by cl/103334620 can be fixed.
    The direct cause of the size regression is that making
    Byte/Character/Float/Integer/Long/Short implement Comparable,
    and each Comparable.$markImplementor(...) was not stripped, even though
    they are not called. See cl/103587701 for a repro case and a deeper
    investigation of the cause.

## Tracking Closure warning issues
- https://b.corp.google.com/hotlists/269212
- b/20102666 WARNING - actual parameter of Foo.$markImplementor does not match
  formal parameter

## TODOs
- anonymousclassforfunctionalinterface (transpiled to function)
- arithmetic exception for integers (e.g. divide by zero / mod by zero)
- arithmetic underflow/overflow
- catchjsexception
- devirtualized classes should skip Object trampoline when possible:
    string.toString()
- enum special functions (values, valueOf, etc).
- enumoptimization
- finalfield (readable JsDoc and no static setter)
- identity equality (double equality / triple equality, null / undefined)
- implicitconversions (String + operator)
- interfacedefaultmethod
- jsinteropclass
- jsinteropdispatch
- jsinteropinterface
- localvariablejsdoc
- methodreferences
- nativefunction + provided Foo.native.js
- objectmethods
- packageclasscollision
- primitiveoverflow
- private static clinit optimization
- qualifiedsupermethodcall
- shorterfieldmangling (for fields in final classes, like enums)
- simpleenum
- stringconversion(nullstringinstances)
- string constructor needs to be implemented
