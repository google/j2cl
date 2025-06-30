#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/DefaultNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/EnumNames.h"

/** J2KT interop test for ObjC. */
@interface J2ktObjCInteropTest : XCTestCase
@end

@implementation J2ktObjCInteropTest

- (void)testDefaultNames {
  J2ktJ2ktiosinteropDefaultNames *obj;
  obj = [[J2ktJ2ktiosinteropDefaultNames alloc] init];
  obj = [[J2ktJ2ktiosinteropDefaultNames alloc] initWithInt:1];
  obj = [[J2ktJ2ktiosinteropDefaultNames alloc] initWithInt:1 withNSString:@""];

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
  [obj objectMethodWithId:NULL];
  [obj stringMethodWithNSString:@""];
  [obj stringArrayMethodWithNSStringArray:NULL];
  [obj stringArrayArrayMethodWithNSStringArray2:NULL];
  [obj cloneableMethodWithNSCopying:NULL];
  [obj numberMethodWithNSNumber:NULL];
  [obj classMethodWithIOSClass:NULL];
  [obj stringIterableMethodWithJavaLangIterable:NULL];
  [obj intStringMethodWithInt:1 withNSString:@""];

  [obj genericMethodWithId:NULL];
  [obj genericStringMethodWithNSString:@""];

  [obj overloadedMethodWithId:NULL];
  [obj overloadedMethodWithInt:1];
  [obj overloadedMethodWithLong:1];

  [obj overloadedMethodWithFloat:1];
  [obj overloadedMethodWithDouble:1];
  [obj overloadedMethodWithNSString:@""];

  int i;

  i = obj.finalIntField_;
  i = obj.intField_;
  obj.intField_ = i;

  i = J2ktJ2ktiosinteropDefaultNamesCompanion.shared.STATIC_FINAL_INT_FIELD_;
  i = J2ktJ2ktiosinteropDefaultNamesCompanion.shared.staticIntField_;
  J2ktJ2ktiosinteropDefaultNamesCompanion.shared.staticIntField_ = i;

  i = J2ktiosinteropDefaultNames_get_STATIC_FINAL_INT_FIELD();
  i = J2ktiosinteropDefaultNames_get_staticIntField();
  J2ktiosinteropDefaultNames_set_staticIntField(i);

  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticMethod];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticIntMethodWithInt:1];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticIntStringMethodWithInt:1 withNSString:@""];

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticIntMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticIntStringMethodWithInt_withNSString_(1, @"");
}

- (void)testSpecialNames {
  [[[J2ktJ2ktiosinteropSpecialNames_WithBoolean alloc] init] getWithBoolean:YES];
  [[[J2ktJ2ktiosinteropSpecialNames_WithChar alloc] init] getWithChar:'a'];
  [[[J2ktJ2ktiosinteropSpecialNames_WithByte alloc] init] getWithByte:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithShort alloc] init] getWithShort:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithInt alloc] init] getWithInt:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithLong alloc] init] getWithLong:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithFloat alloc] init] getWithFloat:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithDouble alloc] init] getWithDouble:1];
  [[[J2ktJ2ktiosinteropSpecialNames_WithObject alloc] init] getWithId:NULL];
  [[[J2ktJ2ktiosinteropSpecialNames_WithString alloc] init] getWithNSString:@""];
  [[[J2ktJ2ktiosinteropSpecialNames_WithFoo alloc] init]
      getWithJ2ktiosinteropSpecialNames_Foo:NULL];
}

- (void)testCustomNames {
  J2ktCustom *obj;
  obj = [[J2ktCustom alloc] initWithIndex:1];
  obj = [[J2ktCustom alloc] initWithIndex:1 name:@""];

  obj = [[J2ktCustom alloc] init];
  // TODO(b/400390599): Should be `init2WithLong:1`
  obj = [[J2ktCustom alloc] initWithLong:1];
  // TODO(b/400390599): Should be `init3WithLong:withNSString`
  obj = [[J2ktCustom alloc] initWithLong:1 withNSString:@""];

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

  [CustomCompanion.shared customStaticMethod];
  [CustomCompanion.shared customStaticIntMethodWithIndex:1];
  [CustomCompanion.shared customStaticIntStringMethodWithIndex:1 name:@""];

  [CustomCompanion.shared customStaticLongMethodWithLong:1];
  [CustomCompanion.shared customStaticLongStringMethodWithLong:1 withNSString:@""];

  Custom_customStaticMethod();
  Custom_customStaticIntMethodWithIndex_(1);
  Custom_customStaticIntStringMethodWithIndex_name_(1, @"");

  Custom_customStaticLongMethod(1);
  Custom_customStaticLongStringMethod(2, @"");
}

- (void)testEnumNames {
  J2ktJ2ktiosinteropEnumNames *e;
  e = J2ktJ2ktiosinteropEnumNames.ONE;
  e = J2ktJ2ktiosinteropEnumNames.TWO;

  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;
}

@end
