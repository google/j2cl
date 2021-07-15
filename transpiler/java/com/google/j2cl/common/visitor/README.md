# J2CL Visitor Patterns

---

J2Cl uses two main visitor classes;

- `AbstractRewriter`
- `AbstractVisitor`

Visitors visit all `@Visitable` nodes in order of how they are declared in
their parent class.

```Java
@Visitable
public class IfStatement extends Statement {
  @Visitable Expression conditionExpression;
  @Visitable Statement thenStatement;
  @Visitable @Nullable Statement elseStatement;
```

in this snippet, the order of visiting an `IfStatement`'s children nodes would be:
`conditionExpression` to `thenStatement` to `elseStatement`.

Note: when creating a Node class in the AST, the order of subnode declaration
should correspond to their evaluation order. This is neccesary for visitor
correctness.

#### Visitor Methods

By default, any visitor method on a node delegates to the corresponding method
on its superclass. Overriding such a method will remove this implicit
delegation. If you would like to preserve that in rare cases you can explicit
call `super.method[_NodeName_]` to continue triggering the chain.

### AbstractRewriter

---

This visitor is commonly used to rewrite the AST by replacing Nodes. An example
use case would be to get rid of statements with no effect on the code (see
transpiler/java/com/google/j2cl/transpiler/passes/RemoveNoopStatements.java) .

#### Method Naming Convention

- **`shouldProcess[_NodeName_]`:** Decides whether to process a subtree of the AST.
  Returns `false` to skip subtree, `true` otherwise. <br>_Top Level Default Behavior: return `true`._
- **`rewrite[_NodeName_]`**: Method to replace a node on the subtree. Returns a
  replacement node for the processed AST node. <br>_Top Level Default Behavior: return the original node._

#### Node Access Pattern

The `AbstractRewriter` generally only lets you traverses the AST in postorder
because the rewrite method is called when leaving a node. Each node is visited
in the following manner;

1. Call the `shouldProcess` method associated with that node.
2. If the `shouldProcess` method returns `false` the
   `AbstractRewriter` leaves the node else it visits the node's children.
3. When the `AbstractRewriter` has completed visiting the subtree associated
   with this node, it calls the `rewrite` method associated with the node
4. Leave the current node.

### AbstractVisitor

---

This visitor is commonly used to collect information and take actions that need
to be done with visitors but will not explicitly change any nodes. It can be
likened to a for each loop or map folding that goes in an inorder traversal of
the AST. An example use case would be to verify that variables and labels are
referenced within their scopes. (see transpiler/java/com/google/j2cl/transpiler/passes/RemoveNoopStatements.java)

#### Method Naming Convention

- **`enter[_NodeName_]`:** First thing the visitor does when it visits a node.
  <br>_Top Level Default Behavior: return `true`._
- **`exit[_NodeName_]`**: Method called after visiting a node and it's subtree.
  <br>_Top Level Default Behavior: do nothing._

#### Node Access Pattern

The `AbstractVisitor` lets you traverse the AST in preorder or postorder.
This traversal is similar to the one done by a depth-first search. Each node is
visited in the following manner;

1. Call the `enter` method associated with the current node.
2. If the `enter` method returns `false` the
   `AbstractVisitor` leaves the node else it visits the node's children.
3. When the `AbstractVisitor` has completed visiting the subtree associated with
   this node, it calls the `Exit` method associated with the node.
4. Leave the current node.

TODO(b/191788487): Add an explanation bout @Context and getCurrentBlah.
