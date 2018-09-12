/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.tools.rta.multipleinheritance;

/**
 * This interface inherits 2 times the same method fooBar(). RTA uses the topological depth to
 * choose which method IFooBar inherits. In this case IBar:fooBar(). If it's not the case (RTA finds
 * that IFooBar inherits from IFoo:fooBar()), the algorithm will find that IFoo:fooBar() is
 * overridden in IBar and IBar:fooBar() is overridden in IFoo through IFooBar and that introduces a
 * circle.
 */
interface IFooBar extends IFoo, IBar {}
