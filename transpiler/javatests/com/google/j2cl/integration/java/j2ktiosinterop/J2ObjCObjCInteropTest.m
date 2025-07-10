#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/DefaultNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/EnumNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/Gc.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/Holder.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/SpecialNames.h"

/** J2ObjC interop test for ObjC. */
@interface J2ObjCObjCInteropTest : XCTestCase
@end

@implementation J2ObjCObjCInteropTest

- (void)testDefaultNames {
  J2ktiosinteropDefaultNames *obj;
  obj = [[J2ktiosinteropDefaultNames alloc] init];
  obj = [[J2ktiosinteropDefaultNames alloc] initWithInt:1];
  obj = [[J2ktiosinteropDefaultNames alloc] initWithInt:1 withNSString:@""];

  obj = create_J2ktiosinteropDefaultNames_init();
  obj = create_J2ktiosinteropDefaultNames_initWithInt_(1);
  obj = create_J2ktiosinteropDefaultNames_initWithInt_withNSString_(1, @"");

  obj = new_J2ktiosinteropDefaultNames_init();
  obj = new_J2ktiosinteropDefaultNames_initWithInt_(1);
  obj = new_J2ktiosinteropDefaultNames_initWithInt_withNSString_(1, @"");

  [obj method];
  [obj booleanMethodWithBoolean:YES];
  [obj charMethodWithChar:'a'];
  [obj byteMethodWithByte:1];
  [obj shortMethodWithShort:1];
  [obj intMethodWithInt:1];
  [obj longMethodWithLong:1];
  [obj floatMethodWithFloat:1];
  [obj doubleMethodWithDouble:1];
  [obj objectMethodWithId:nil];
  [obj stringMethodWithNSString:@""];
  [obj stringArrayMethodWithNSStringArray:nil];
  [obj stringArrayArrayMethodWithNSStringArray2:nil];
  [obj cloneableMethodWithNSCopying:nil];
  [obj numberMethodWithNSNumber:nil];
  [obj classMethodWithIOSClass:nil];
  [obj stringIterableMethodWithJavaLangIterable:nil];
  [obj intStringMethodWithInt:1 withNSString:@""];

  [obj genericMethodWithId:nil];
  [obj genericStringMethodWithNSString:@""];

  [obj overloadedMethodWithId:nil];
  [obj overloadedMethodWithInt:1];
  [obj overloadedMethodWithLong:1];

  [obj overloadedMethodWithFloat:1];
  [obj overloadedMethodWithDouble:1];
  [obj overloadedMethodWithNSString:@""];

  int i;

  i = J2ktiosinteropDefaultNames_get_finalIntField();
  i = obj->intField_;
  obj->intField_ = i;

  i = J2ktiosinteropDefaultNames_get_STATIC_FINAL_INT_FIELD();
  i = J2ktiosinteropDefaultNames_get_staticIntField();
  J2ktiosinteropDefaultNames_set_staticIntField(i);

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticIntMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticIntStringMethodWithInt_withNSString_(1, @"");
}

- (void)testSpecialNames {
  [[[J2ktiosinteropSpecialNames_WithBoolean alloc] init] getWithBoolean:YES];
  [[[J2ktiosinteropSpecialNames_WithChar alloc] init] getWithChar:'a'];
  [[[J2ktiosinteropSpecialNames_WithByte alloc] init] getWithByte:1];
  [[[J2ktiosinteropSpecialNames_WithShort alloc] init] getWithShort:1];
  [[[J2ktiosinteropSpecialNames_WithInt alloc] init] getWithInt:1];
  [[[J2ktiosinteropSpecialNames_WithLong alloc] init] getWithLong:1];
  [[[J2ktiosinteropSpecialNames_WithFloat alloc] init] getWithFloat:1];
  [[[J2ktiosinteropSpecialNames_WithDouble alloc] init] getWithDouble:1];
  [[[J2ktiosinteropSpecialNames_WithObject alloc] init] getWithId:nil];
  [[[J2ktiosinteropSpecialNames_WithString alloc] init] getWithNSString:@""];
  [[[J2ktiosinteropSpecialNames_WithFoo alloc] init] getWithJ2ktiosinteropSpecialNames_Foo:nil];
}

- (void)testCustomNames {
  Custom *obj;
  obj = [[Custom alloc] initWithIndex:1];
  obj = [[Custom alloc] initWithIndex:1 name:@""];

  obj = [[Custom alloc] init];
  obj = [[Custom alloc] init2WithLong:1];
  obj = [[Custom alloc] init3WithLong:1 withNSString:@""];

  obj = create_Custom_initWithIndex_(1);
  obj = create_Custom_initWithIndex_name_(1, @"");

  obj = create_Custom_init();
  obj = create_Custom_init2(1);
  obj = create_Custom_init3(1, @"");

  obj = new_Custom_initWithIndex_(1);
  obj = new_Custom_initWithIndex_name_(1, @"");

  obj = new_Custom_init();
  obj = new_Custom_init2(1);
  obj = new_Custom_init3(1, @"");

  [obj customMethod];
  [obj customIntMethodWithInt:1];
  [obj customIndexMethodWithIndex:1];
  [obj customCountMethodWithCount:1];
  [obj customStringMethodWithString:@""];
  [obj customNameMethodWithName:@""];
  [obj customIntStringMethodWithIndex:1 name:@""];

  [obj customLongMethodWithLong:1];
  [obj customLongStringMethodWithLong:1 withNSString:@""];

  Custom_customStaticMethod();
  Custom_customStaticIntMethodWithIndex_(1);
  Custom_customStaticIntStringMethodWithIndex_name_(1, @"");

  Custom_customStaticLongMethod(1);
  Custom_customStaticLongStringMethod(2, @"");
}

- (void)testEnumNames {
  J2ktiosinteropEnumNames *e;
  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;
}

- (void)testCycle_JavaHolder {
  __weak JavaHolder *weakHolder = nil;

  @autoreleasepool {
    @autoreleasepool {
      JavaHolder *holder = [[JavaHolder alloc] initWithId:nil];
      [holder setWithId:holder];

      weakHolder = holder;

      XCTAssertNotNil([holder get]);
      XCTAssertNotNil(weakHolder);
    }

    JavaGc_collect();
    XCTAssertNotNil(weakHolder);

    [weakHolder setWithId:nil];
    XCTAssertNil([weakHolder get]);
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
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);

    [weakHolder setWithId:nil];
    XCTAssertNil([weakHolder get]);
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
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
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
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
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
  XCTAssertNil(weakHolder);
  XCTAssertNil(weakSupplier);
  XCTAssertNil(weakArray);
}

- (void)testCycle_JavaHolder_JavaWeakOuterAnnotationAnonymousSupplier {
  __weak JavaHolder *weakHolder = nil;
  __weak id<JavaSupplier> weakSupplier = nil;

  @autoreleasepool {
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

    // @WeakOuter has no effect on anonymous classes
    JavaGc_collect();
    XCTAssertNotNil(weakHolder);
    XCTAssertNotNil(weakSupplier);

    [weakHolder setWithId:nil];
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
