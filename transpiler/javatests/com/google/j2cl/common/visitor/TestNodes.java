/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.common.visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

@SuppressWarnings("MultipleTopLevelClasses")
final class TestNodes {
  private TestNodes() {}
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class Descriptor {
  final String name;
  @Visitable @Nullable Descriptor childDescriptor;
  int acceptCalls;

  Descriptor(String name, @Nullable Descriptor childDescriptor) {
    this.name = name;
    this.childDescriptor = childDescriptor;
  }

  Descriptor acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_Descriptor.visit(processor, this);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
abstract class TestNode {
  int acceptCalls;

  final void accept(Processor processor) {
    TestNode unused = acceptInternal(processor);
  }

  abstract TestNode acceptInternal(Processor processor);
}

@SuppressWarnings("MultipleTopLevelClasses")
@Context
@Visitable
class RootNode extends TestNode {
  final String name;
  @Visitable LeafANode leafA;
  @Visitable WrapperNode wrapper;
  @Visitable List<TestNode> children;
  @Visitable @Nullable List<TestNode> nullableChildren;
  @Visitable @Nullable LeafBNode nullableLeafB;

  RootNode(
      String name,
      LeafANode leafA,
      WrapperNode wrapper,
      List<TestNode> children,
      @Nullable List<TestNode> nullableChildren,
      @Nullable LeafBNode nullableLeafB) {
    this.name = name;
    this.leafA = leafA;
    this.wrapper = wrapper;
    this.children = new ArrayList<>(children);
    this.nullableChildren = nullableChildren;
    this.nullableLeafB = nullableLeafB;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_RootNode.visit(processor, this);
  }

  @Override
  public String toString() {
    return "RootNode(" + name + ")";
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class ContainerNode extends TestNode {
  @Visitable List<TestNode> items;

  ContainerNode(TestNode... items) {
    this.items = new ArrayList<>(Arrays.asList(items));
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_ContainerNode.visit(processor, this);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class WrapperNode extends TestNode {
  @Visitable Descriptor descriptor;

  WrapperNode(Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_WrapperNode.visit(processor, this);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class LeafANode extends TestNode {
  final String value;

  LeafANode(String value) {
    this.value = value;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_LeafANode.visit(processor, this);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class LeafBNode extends TestNode {
  @Visitable Descriptor descriptor;

  LeafBNode(Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_LeafBNode.visit(processor, this);
  }
}

/** Not {@code @Visitable}: dispatches through the {@code acceptInternal} it inherits. */
@SuppressWarnings("MultipleTopLevelClasses")
final class SpecialLeafANode extends LeafANode {
  SpecialLeafANode(String value) {
    super(value);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class HolderNode extends TestNode {
  @Visitable TestNode held;

  HolderNode(TestNode held) {
    this.held = held;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_HolderNode.visit(processor, this);
  }
}

/**
 * Not {@code @Visitable}: {@link IndirectNode} is a {@link TestNode} only through this class, so
 * the generated visitors treat it as a separate visitable hierarchy.
 */
@SuppressWarnings("MultipleTopLevelClasses")
abstract class NonVisitableIntermediateNode extends TestNode {}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class IndirectNode extends NonVisitableIntermediateNode {
  final String name;

  IndirectNode(String name) {
    this.name = name;
  }

  @Override
  TestNode acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_IndirectNode.visit(processor, this);
  }
}

@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
interface VisitableInterface {
  VisitableInterface acceptInternal(Processor processor);
}

/** A visitable class whose only visitable supertype is an interface. */
@SuppressWarnings("MultipleTopLevelClasses")
@Visitable
class InterfaceImplNode implements VisitableInterface {
  @Visitable LeafANode leaf;
  int acceptCalls;

  InterfaceImplNode(LeafANode leaf) {
    this.leaf = leaf;
  }

  @Override
  public VisitableInterface acceptInternal(Processor processor) {
    acceptCalls++;
    return Visitor_InterfaceImplNode.visit(processor, this);
  }
}
