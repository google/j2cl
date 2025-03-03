#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/DefaultNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/EnumNames.h"

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
  [obj methodWithBoolean:YES];
  [obj methodWithChar:'a'];
  [obj methodWithByte:1];
  [obj methodWithShort:1];
  [obj methodWithInt:1];
  [obj methodWithLong:1];
  [obj methodWithFloat:1];
  [obj methodWithDouble:1];
  [obj methodWithId:NULL];
  [obj methodWithNSString:NULL];
  [obj methodWithNSStringArray:NULL];
  [obj methodWithNSStringArray2:NULL];
  [obj methodWithNSCopying:NULL];
  [obj methodWithNSNumber:NULL];
  [obj methodWithIOSClass:NULL];
  [obj methodWithJavaLangIterable:NULL];
  [obj methodWithInt:1 withNSString:NULL];

  [obj genericMethodWithId:NULL];
  [obj genericMethodWithNSString:NULL];

  obj->field_ = obj->field_ + 1;

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticMethodWithInt_withNSString_(1, @"");
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

  [obj custom];
  [obj customWithIndex:1];
  [obj customWithIndex:1 name:@""];

  [obj customWithLong:1];
  [obj customWithLong:1 withNSString:@""];

  Custom_staticCustom();
  Custom_staticCustomWithIndex_(1);
  Custom_staticCustomWithIndex_name_(1, @"");

  Custom_staticCustom2(1);
  Custom_staticCustom3(2, @"");
}

- (void)testEnumNames {
  J2ktiosinteropEnumNames *e;
  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;
}

@end
