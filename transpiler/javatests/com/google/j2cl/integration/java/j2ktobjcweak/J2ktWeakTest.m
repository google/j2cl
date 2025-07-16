#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktobjcweak/Gc.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktobjcweak/Holder.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktobjcweak/Supplier.h"

/** J2KT weak test. */
@interface J2ktWeakTest : XCTestCase
@end

@implementation J2ktWeakTest

- (void)testCycle_JavaHolder {
  __weak JavaHolder *weakHolder = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    [holder setWithId:holder];

    weakHolder = holder;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
}

- (void)testCycle_JavaHolder_NSArray {
  __weak JavaHolder *weakHolder = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    @autoreleasepool {
      JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
      NSArray *array = [NSArray arrayWithObject:holder];
      [holder setWithId:array];

      weakHolder = holder;
      weakArray = array;

      XCTAssertNotNil([holder get]);
      XCTAssertNotNil(weakHolder);
      XCTAssertNotNil(weakArray);
    }

    JavaGc_collect();
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakArray);

    [weakHolder setWithId:nil];
    XCTAssertNil([weakHolder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    @autoreleasepool {
      JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
      id<JavaSupplier> supplier = [holder newSupplier];
      NSArray *array = [NSArray arrayWithObject:supplier];
      [holder setWithId:array];

      weakHolder = holder;
      weakSupplier = supplier;
      weakArray = array;

      XCTAssertNotNil([holder get]);
      XCTAssertNotNil(weakHolder);
      XCTAssertNotNil(weakSupplier);
      XCTAssertNotNil(weakArray);
    }

    JavaGc_collect();
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);

    [weakHolder setWithId:nil];
    XCTAssertNil([weakHolder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakReferenceSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakReferenceSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaWeakReferenceSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakReferenceSupplier];
    NSArray *array = [NSArray arrayWithObject:supplier];
    [holder setWithId:array];

    weakHolder = holder;
    weakSupplier = supplier;
    weakArray = array;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakAnnotationSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakAnnotationSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaWeakAnnotationSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakAnnotationSupplier];
    NSArray *array = [NSArray arrayWithObject:supplier];
    [holder setWithId:array];

    weakHolder = holder;
    weakSupplier = supplier;
    weakArray = array;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  // TODO(b/428900562): These should all be nil when @Weak annotation is
  // supported.
  XCTAssertNotNil(weakHolder);
  XCTAssertNotNil(weakSupplier);
  XCTAssertNotNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakOuterAnnotationSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakOuterAnnotationSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaWeakOuterAnnotationSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakOuterAnnotationSupplier];
    NSArray *array = [NSArray arrayWithObject:supplier];
    [holder setWithId:array];

    weakHolder = holder;
    weakSupplier = supplier;
    weakArray = array;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  // TODO(b/428900562): These should all be nil when @WeakOuter annotation is
  // supported.
  XCTAssertNotNil(weakHolder);
  XCTAssertNotNil(weakSupplier);
  XCTAssertNotNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakOuterAnnotationLambdaSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakOuterAnnotationLambdaSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaWeakOuterAnnotationLambdaSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakOuterAnnotationLambdaSupplier];
    NSArray *array = [NSArray arrayWithObject:supplier];
    [holder setWithId:array];

    weakHolder = holder;
    weakSupplier = supplier;
    weakArray = array;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  // TODO(b/428900562): These should all be nil when @WeakOuter annotation is
  // supported.
  XCTAssertNotNil(weakHolder);
  XCTAssertNotNil(weakSupplier);
  XCTAssertNotNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakOuterAnnotationAnonymousSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
    JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
    id<JavaSupplier> supplier = [holder newWeakOuterAnnotationAnonymousSupplier];
    [holder setWithId:supplier];

    weakHolder = holder;
    weakSupplier = supplier;

    XCTAssertNotNil([holder get]);
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
}

- (void)testCycle_JavaHolder_NSArray_JavaWeakOuterAnnotationAnnonymousSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;
  __weak NSArray *weakArray = nil;

  @autoreleasepool {
    @autoreleasepool {
      JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
      id<JavaSupplier> supplier = [holder newWeakOuterAnnotationAnonymousSupplier];
      NSArray *array = [NSArray arrayWithObject:supplier];
      [holder setWithId:array];

      weakHolder = holder;
      weakSupplier = supplier;
      weakArray = array;

      XCTAssertNotNil([holder get]);
      XCTAssertNotNil(weakHolder);
      XCTAssertNotNil(weakSupplier);
      XCTAssertNotNil(weakArray);
    }

    // @WeakOuter has no effect on anonymous classes
    JavaGc_collect();
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);

    [weakHolder setWithId:nil];
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);
    XCTAssertNotNil(weakArray);
  }

  JavaGc_collect();
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
}

@end
