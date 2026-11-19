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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class VisitorTest {

  @Test
  public void skipsUnvisitedFieldsAndSubtrees() {
    Descriptor nestedDescriptor = new Descriptor("nestedDesc", null);
    Descriptor wrapperDescriptor = new Descriptor("wrapperDesc", nestedDescriptor);
    WrapperNode wrapper = new WrapperNode(wrapperDescriptor);
    Descriptor leafBDescriptor = new Descriptor("leafBDesc", null);
    LeafBNode leafB = new LeafBNode(leafBDescriptor);
    LeafANode topLeafA = new LeafANode("top");
    LeafANode nestedLeafA = new LeafANode("nested");
    ContainerNode container = new ContainerNode(nestedLeafA);
    RootNode root = newRootNode(topLeafA, wrapper, leafB, container);

    List<String> visited = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterLeafANode(LeafANode node) {
            visited.add(node.value);
            return true;
          }
        });

    assertThat(visited).containsExactly("top", "nested").inOrder();
    // WrapperNode and its Descriptor subtree cannot reach LeafANode, so the field is skipped.
    assertThat(wrapper.acceptCalls).isEqualTo(0);
    assertThat(wrapperDescriptor.acceptCalls).isEqualTo(0);
    assertThat(nestedDescriptor.acceptCalls).isEqualTo(0);
    // LeafBNode is in List<TestNode> so acceptInternal is invoked, but Visitor_LeafBNode returns
    // immediately without visiting its Descriptor child.
    assertThat(leafB.acceptCalls).isEqualTo(1);
    assertThat(leafBDescriptor.acceptCalls).isEqualTo(0);
  }

  @Test
  public void noOpVisitorShortCircuitsAtRoot() {
    WrapperNode wrapper = new WrapperNode(new Descriptor("desc", null));
    LeafANode leafA = new LeafANode("top");
    RootNode root = newRootNode(leafA, wrapper);

    root.accept(new AbstractVisitor() {});
    root.accept(new AbstractRewriter() {});

    assertThat(root.acceptCalls).isEqualTo(2);
    assertThat(leafA.acceptCalls).isEqualTo(0);
    assertThat(wrapper.acceptCalls).isEqualTo(0);
  }

  @Test
  public void supertypeCallbackVisitsSubtypesAndSkipsUnrelatedHierarchy() {
    Descriptor wrapperDescriptor = new Descriptor("wrapperDesc", null);
    WrapperNode wrapper = new WrapperNode(wrapperDescriptor);
    Descriptor leafBDescriptor = new Descriptor("leafBDesc", null);
    LeafBNode leafB = new LeafBNode(leafBDescriptor);
    LeafANode leafA = new LeafANode("top");
    RootNode root = newRootNode(leafA, wrapper, leafB);

    List<Class<?>> visitedTypes = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterTestNode(TestNode node) {
            visitedTypes.add(node.getClass());
            return true;
          }
        });

    assertThat(visitedTypes)
        .containsExactly(RootNode.class, LeafANode.class, WrapperNode.class, LeafBNode.class)
        .inOrder();
    // Descriptor is an independent @Visitable root, so it is still skipped.
    assertThat(wrapperDescriptor.acceptCalls).isEqualTo(0);
    assertThat(leafBDescriptor.acceptCalls).isEqualTo(0);
  }

  @Test
  public void recursiveDescriptorReachabilityTraversesOnlyPathsToDescriptor() {
    Descriptor inner = new Descriptor("inner", null);
    Descriptor outer = new Descriptor("outer", inner);
    WrapperNode wrapper = new WrapperNode(outer);
    LeafANode leafA = new LeafANode("top");
    Descriptor leafBDesc = new Descriptor("fromLeafB", null);
    LeafBNode leafB = new LeafBNode(leafBDesc);
    RootNode root = newRootNode(leafA, wrapper, leafB);

    List<String> exitedDescriptors = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public void exitDescriptor(Descriptor descriptor) {
            exitedDescriptors.add(descriptor.name);
          }
        });

    assertThat(exitedDescriptors).containsExactly("inner", "outer", "fromLeafB").inOrder();
    // LeafANode cannot reach Descriptor, so root.leafA is skipped completely.
    assertThat(leafA.acceptCalls).isEqualTo(0);
  }

  @Test
  public void enterReturningFalseSkipsChildrenButInvokesExit() {
    LeafANode skippedChild = new LeafANode("skipped");
    ContainerNode container = new ContainerNode(skippedChild);
    RootNode root = newRootNodeWithChildren(container);

    List<String> events = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterContainerNode(ContainerNode node) {
            events.add("enterContainer");
            return false;
          }

          @Override
          public void exitContainerNode(ContainerNode node) {
            events.add("exitContainer");
          }

          @Override
          public boolean enterLeafANode(LeafANode node) {
            events.add("leaf:" + node.value);
            return true;
          }
        });

    assertThat(events).containsExactly("leaf:top", "enterContainer", "exitContainer").inOrder();
    assertThat(skippedChild.acceptCalls).isEqualTo(0);
  }

  private abstract static class BaseCustomVisitor extends AbstractVisitor {
    final List<String> events = new ArrayList<>();

    @Override
    public boolean enterContainerNode(ContainerNode node) {
      events.add("baseEnterContainer");
      return true;
    }
  }

  @Test
  public void multiLevelVisitorSubclassInheritsParentOverrides() {
    LeafANode nested = new LeafANode("nested");
    ContainerNode container = new ContainerNode(nested);
    RootNode root = newRootNodeWithChildren(container);

    BaseCustomVisitor visitor =
        new BaseCustomVisitor() {
          @Override
          public void exitLeafANode(LeafANode node) {
            events.add("exitLeaf:" + node.value);
          }
        };
    root.accept(visitor);

    assertThat(visitor.events)
        .containsExactly("exitLeaf:top", "baseEnterContainer", "exitLeaf:nested")
        .inOrder();
  }

  @Test
  public void rewriterReplacesNodesRemovesListElementsAndHandlesNullables() {
    LeafANode topLeaf = new LeafANode("top");
    LeafBNode removedLeaf = new LeafBNode(new Descriptor("d", null));
    LeafANode listLeaf = new LeafANode("inList");
    List<TestNode> nullableList =
        new ArrayList<>(ImmutableList.of(new LeafANode("inNullableList")));
    RootNode root =
        new RootNode(
            "main",
            topLeaf,
            new WrapperNode(new Descriptor("w", null)),
            ImmutableList.of(removedLeaf, listLeaf),
            nullableList,
            null);

    root.accept(
        new AbstractRewriter() {
          @Override
          public TestNode rewriteLeafANode(LeafANode node) {
            return new LeafANode(node.value + "_rewritten");
          }

          @Override
          public TestNode rewriteLeafBNode(LeafBNode node) {
            return null;
          }
        });

    assertThat(root.leafA.value).isEqualTo("top_rewritten");
    assertThat(root.children).hasSize(1);
    assertThat(((LeafANode) root.children.get(0)).value).isEqualTo("inList_rewritten");
    assertThat(((LeafANode) root.nullableChildren.get(0)).value)
        .isEqualTo("inNullableList_rewritten");
    assertThat(root.nullableLeafB).isNull();
  }

  @Test
  public void rewriterRejectsNullForNonNullableScalarField() {
    RootNode root = newRootNodeWithChildren();

    AbstractRewriter rewriter =
        new AbstractRewriter() {
          @Override
          public TestNode rewriteLeafANode(LeafANode node) {
            return null;
          }
        };

    ProcessorError error = assertThrows(ProcessorError.class, () -> root.accept(rewriter));
    assertThat(error).hasCauseThat().isInstanceOf(NullPointerException.class);
  }

  @Test
  public void contextAndParentStacksArePopulatedAndUsedForErrors() {
    LeafANode nested = new LeafANode("nested");
    ContainerNode container = new ContainerNode(nested);
    RootNode root = newRootNodeWithChildren(container);

    Object[] capturedParent = new Object[1];
    RootNode[] capturedContext = new RootNode[1];
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterLeafANode(LeafANode node) {
            if (node.value.equals("nested")) {
              capturedParent[0] = getParent();
              capturedContext[0] = getCurrentRootNode();
            }
            return true;
          }
        });

    assertThat(capturedParent[0]).isSameInstanceAs(container);
    assertThat(capturedContext[0]).isSameInstanceAs(root);

    AbstractVisitor failingVisitor =
        new AbstractVisitor() {
          @Override
          public boolean enterLeafANode(LeafANode node) {
            if (node.value.equals("nested")) {
              throw new IllegalStateException("expected failure");
            }
            return true;
          }
        };

    ProcessorError error = assertThrows(ProcessorError.class, () -> root.accept(failingVisitor));
    assertThat(error).hasMessageThat().contains("RootNode(main)");
    assertThat(error).hasCauseThat().hasCauseThat().isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void helperMethodsNamedLikeCallbacksDoNotTriggerTraversal() {
    LeafANode leafA = new LeafANode("top");
    WrapperNode wrapper = new WrapperNode(new Descriptor("w", null));
    RootNode root = newRootNode(leafA, wrapper);

    // The methods are public on purpose: only public methods can be callbacks, so this checks that
    // the parameter types are taken into account and not just the names.
    root.accept(
        new AbstractVisitor() {
          @SuppressWarnings("unused")
          public boolean enterLeafANode(String value) {
            throw new AssertionError("Not a visitor callback");
          }

          @SuppressWarnings("unused")
          public boolean enterLeafANode(LeafANode node, int depth) {
            throw new AssertionError("Not a visitor callback");
          }

          @SuppressWarnings("unused")
          public void exitWrapperNode(TestNode node) {
            throw new AssertionError("Not a visitor callback");
          }
        });

    assertThat(root.acceptCalls).isEqualTo(1);
    assertThat(leafA.acceptCalls).isEqualTo(0);
    assertThat(wrapper.acceptCalls).isEqualTo(0);
  }

  @Test
  public void polymorphicChildrenArePrunedBasedOnTheirRuntimeType() {
    SpecialLeafANode special = new SpecialLeafANode("special");
    Descriptor listWrapperDescriptor = new Descriptor("listWrapperDesc", null);
    WrapperNode listWrapper = new WrapperNode(listWrapperDescriptor);
    LeafANode nested = new LeafANode("nested");
    ContainerNode container = new ContainerNode(nested);
    RootNode root = newRootNodeWithChildren(special, listWrapper, container);

    List<String> visited = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterLeafANode(LeafANode node) {
            visited.add(node.value);
            return true;
          }
        });

    // The non-@Visitable subclass is visited through the acceptInternal it inherits.
    assertThat(visited).containsExactly("top", "special", "nested").inOrder();
    // WrapperNode cannot reach LeafANode: as an element of a List<TestNode> it is dispatched, but
    // it returns without visiting its Descriptor.
    assertThat(listWrapper.acceptCalls).isEqualTo(1);
    assertThat(listWrapperDescriptor.acceptCalls).isEqualTo(0);
    assertThat(container.acceptCalls).isEqualTo(1);
  }

  @Test
  public void supertypeCallbacksSupportSuperDelegation() {
    RootNode root = newRootNodeWithChildren(new LeafBNode(new Descriptor("b", null)));

    List<String> events = new ArrayList<>();
    root.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterTestNode(TestNode node) {
            events.add("enter:" + node.getClass().getSimpleName());
            return true;
          }

          @Override
          public boolean enterLeafANode(LeafANode node) {
            events.add("enterLeafA:" + node.value);
            return super.enterLeafANode(node);
          }

          @Override
          public void exitTestNode(TestNode node) {
            events.add("exit:" + node.getClass().getSimpleName());
          }
        });

    assertThat(events)
        .containsExactly(
            "enter:RootNode",
            "enterLeafA:top",
            "enter:LeafANode",
            "exit:LeafANode",
            "enter:WrapperNode",
            "exit:WrapperNode",
            "enter:LeafBNode",
            "exit:LeafBNode",
            "exit:RootNode")
        .inOrder();
  }

  @Test
  public void rewriterWithCovariantReturnTypes() {
    LeafANode keptLeaf = new LeafANode("keep");
    RootNode root =
        newRootNode(
            keptLeaf,
            new WrapperNode(new Descriptor("w", null)),
            new LeafANode("remove"),
            new LeafANode("rename"));

    root.accept(
        new AbstractRewriter() {
          @Override
          public LeafANode rewriteLeafANode(LeafANode node) {
            return switch (node.value) {
              case "remove" -> null;
              case "rename" -> new LeafANode("renamed");
              default -> node;
            };
          }

          @Override
          public WrapperNode rewriteWrapperNode(WrapperNode node) {
            return new WrapperNode(new Descriptor("replaced", null));
          }
        });

    assertThat(root.leafA).isSameInstanceAs(keptLeaf);
    assertThat(root.wrapper.descriptor.name).isEqualTo("replaced");
    assertThat(root.children).hasSize(1);
    assertThat(((LeafANode) root.children.get(0)).value).isEqualTo("renamed");
  }

  @Test
  public void visitableInterfaceParentDispatchesCallbacksAndPrunesFields() {
    LeafANode leaf = new LeafANode("interfaceLeaf");
    InterfaceImplNode impl = new InterfaceImplNode(leaf);

    List<VisitableInterface> entered = new ArrayList<>();
    VisitableInterface result =
        impl.acceptInternal(
            new AbstractVisitor() {
              @Override
              public boolean enterVisitableInterface(VisitableInterface node) {
                entered.add(node);
                return true;
              }
            });

    assertThat(result).isSameInstanceAs(impl);
    assertThat(entered).containsExactly(impl);
    // LeafANode cannot reach the VisitableInterface callback, so the field is skipped.
    assertThat(leaf.acceptCalls).isEqualTo(0);

    List<String> visitedLeaves = new ArrayList<>();
    VisitableInterface secondResult =
        impl.acceptInternal(
            new AbstractVisitor() {
              @Override
              public boolean enterLeafANode(LeafANode node) {
                visitedLeaves.add(node.value);
                return true;
              }
            });

    assertThat(secondResult).isSameInstanceAs(impl);
    assertThat(visitedLeaves).containsExactly("interfaceLeaf");
    assertThat(leaf.acceptCalls).isEqualTo(1);
    assertThat(impl.acceptCalls).isEqualTo(2);
  }

  @Test
  public void subclassOfNonVisitableIntermediateClassIsVisitedThroughAncestorTypedField() {
    IndirectNode indirect = new IndirectNode("indirect");
    HolderNode holder = new HolderNode(indirect);

    List<String> entered = new ArrayList<>();
    holder.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterIndirectNode(IndirectNode node) {
            entered.add(node.name);
            return true;
          }
        });

    // IndirectNode is not a visitable subtype of TestNode, but a field of type TestNode can hold
    // it, so the field has to be visited for the callback to be invoked.
    assertThat(entered).containsExactly("indirect");
    assertThat(indirect.acceptCalls).isEqualTo(1);
  }

  @Test
  public void rewriterOverridingOnlyShouldProcessIsDetected() {
    LeafANode skippedChild = new LeafANode("skipped");
    ContainerNode container = new ContainerNode(skippedChild);
    RootNode root = newRootNodeWithChildren(container);

    List<String> events = new ArrayList<>();
    root.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessContainerNode(ContainerNode node) {
            events.add("shouldProcessContainer");
            return false;
          }
        });

    assertThat(events).containsExactly("shouldProcessContainer");
    assertThat(container.acceptCalls).isEqualTo(1);
    assertThat(skippedChild.acceptCalls).isEqualTo(0);
    // Neither LeafANode nor WrapperNode can reach a ContainerNode, so these fields are skipped.
    assertThat(root.leafA.acceptCalls).isEqualTo(0);
    assertThat(root.wrapper.acceptCalls).isEqualTo(0);
  }

  @Test
  public void rewriterReplacesAndRemovesNonNullNullableScalarField() {
    LeafBNode originalLeafB = new LeafBNode(new Descriptor("original", null));
    RootNode root =
        new RootNode(
            "main",
            new LeafANode("top"),
            new WrapperNode(new Descriptor("w", null)),
            ImmutableList.of(),
            null,
            originalLeafB);

    root.accept(
        new AbstractRewriter() {
          @Override
          public TestNode rewriteLeafBNode(LeafBNode node) {
            return new LeafBNode(new Descriptor("replaced", null));
          }
        });

    assertThat(originalLeafB.acceptCalls).isEqualTo(1);
    assertThat(root.nullableLeafB.descriptor.name).isEqualTo("replaced");

    root.accept(
        new AbstractRewriter() {
          @Override
          public TestNode rewriteLeafBNode(LeafBNode node) {
            return null;
          }
        });

    assertThat(root.nullableLeafB).isNull();
  }

  @Test
  public void fieldReassignedDuringTheTraversalOfItsNodeIsKept() {
    LeafANode originalLeafA = new LeafANode("original");
    LeafANode replacementLeafA = new LeafANode("replacement");
    RootNode root = newRootNode(originalLeafA, new WrapperNode(new Descriptor("w", null)));

    root.accept(
        new AbstractVisitor() {
          @Override
          public void exitLeafANode(LeafANode node) {
            if (node.value.equals("original")) {
              ((RootNode) getParent()).leafA = replacementLeafA;
            }
          }
        });

    // The visited node was not replaced, so the field is not written back and the reassignment
    // made by the callback is kept.
    assertThat(root.leafA).isSameInstanceAs(replacementLeafA);
  }

  @Test
  public void processorClassesInTheSameHierarchyHaveTheirOwnFlags() {
    Descriptor descriptor = new Descriptor("w", null);
    RootNode root = newRootNode(new LeafANode("top"), new WrapperNode(descriptor));

    List<String> events = new ArrayList<>();
    class BaseVisitor extends AbstractVisitor {
      @Override
      public void exitLeafANode(LeafANode node) {
        events.add("leaf:" + node.value);
      }
    }
    class DerivedVisitor extends BaseVisitor {
      @Override
      public void exitDescriptor(Descriptor node) {
        events.add("descriptor:" + node.name);
      }
    }

    root.accept(new BaseVisitor());
    root.accept(new DerivedVisitor());
    root.accept(new BaseVisitor());

    assertThat(events)
        .containsExactly("leaf:top", "leaf:top", "descriptor:w", "leaf:top")
        .inOrder();
    // Only DerivedVisitor can reach the Descriptor.
    assertThat(descriptor.acceptCalls).isEqualTo(1);
  }

  /** Returns a root node with the given children and default values for the other fields. */
  private static RootNode newRootNodeWithChildren(TestNode... children) {
    return newRootNode(new LeafANode("top"), new WrapperNode(new Descriptor("w", null)), children);
  }

  private static RootNode newRootNode(LeafANode leafA, WrapperNode wrapper, TestNode... children) {
    return new RootNode("main", leafA, wrapper, ImmutableList.copyOf(children), null, null);
  }
}
