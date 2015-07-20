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

## Tracking Closure warning issues
- https://b.corp.google.com/hotlists/269212
- b/20102666 WARNING - actual parameter of Foo.$markImplementor does not match
  formal parameter

## Useful tools/testings to add
- testing the cost of adding more parents in a hierarchy.

## Likely readable examples to add (remove some redundant)
- anonymousclassforfunctionalinterface (transpiled to function)
- boxing
- breakwithlabel
- bridgemethodaccidentaloverride
- bridgemethodboundedtypes
- bridgemethodmultipleoverrides
- bridgemethodmultipletypevars
- casttoprimitive
- catchjsexception
- classlocalvarcollision
- classliteralForEnums (tests are in classliteral)
- complextypeinference
- continuewithlabel
- enumwithanonymousclass
- enum special functions (values, valueOf, etc).
- equals
- finalfield (readable JsDoc and no static setter)
- genericconstructorofgenericclass
- genericconstructorofnongenericclass
- genericinstancemethod
- implementgenericinterface
- implementgenericinterfacewithparam
- implicitconversions
- instanceofarray
- interfacedefaultmethod
- jsinteropclass
- jsinteropdispatch
- jsinteropinterface
- methodreferences
- multipleparametersgenerictype
- nativefunction + provided Foo.native.js
- notimportinternals
- numbermethods
- number devirtualization
- objectmethods
- packageclasscollision
- packageprivateinstancemethods
- primitiveoverflow
- simplebridgemethod
- simpleenum
- simplegenerictype
- simplelambda
- staticfieldaccess (readable example + warning log)
- subclassgenericclass
- subclassgenericclasswithparam
- typeinference
- typewildcards
