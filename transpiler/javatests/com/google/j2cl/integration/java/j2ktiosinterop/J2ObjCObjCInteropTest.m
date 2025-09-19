#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/DefaultNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/EnumNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/OnlyExplicitDefaultConstructor.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/OnlyImplicitDefaultConstructor.h"
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
  [obj genericArrayMethodWithNSObjectArray:nil];
  [obj genericStringArrayMethodWithNSStringArray:nil];
  [obj cloneableMethodWithNSCopying:nil];
  [obj numberMethodWithNSNumber:nil];
  [obj classMethodWithIOSClass:nil];
  [obj stringIterableMethodWithJavaLangIterable:nil];
  [obj intStringMethodWithInt:1 withNSString:@""];
  [obj customNamesMethodWithCustom:nil];
  [obj defaultNamesMethodWithJ2ktiosinteropDefaultNames:nil];

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

- (void)testOnlyImplicitDefaultConstructor {
  J2ktiosinteropOnlyImplicitDefaultConstructor *obj;
  obj = [[J2ktiosinteropOnlyImplicitDefaultConstructor alloc] init];
  obj = create_J2ktiosinteropOnlyImplicitDefaultConstructor_init();
  obj = new_J2ktiosinteropOnlyImplicitDefaultConstructor_init();
}

- (void)testOnlyExplicitDefaultConstructor {
  J2ktiosinteropOnlyExplicitDefaultConstructor *obj;
  obj = [[J2ktiosinteropOnlyExplicitDefaultConstructor alloc] init];
  obj = create_J2ktiosinteropOnlyExplicitDefaultConstructor_init();
  obj = new_J2ktiosinteropOnlyExplicitDefaultConstructor_init();
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

  [obj customCustomNamesMethodWithCustom:nil];
  [obj customDefaultNamesMethodWithJ2ktiosinteropDefaultNames:nil];

  [obj customObjectiveCSwiftStringMethodWithString:@""];
  [obj swiftStringMethodWithNSString:@""];

  Custom_customStaticMethod();
  Custom_customStaticIntMethodWithIndex_(1);
  Custom_customStaticIntStringMethodWithIndex_name_(1, @"");

  Custom_customStaticLongMethod(1);
  Custom_customStaticLongStringMethod(2, @"");

  [obj lowercase:@""];
  Custom_staticlowercase_(@"");
}

- (void)testEnumNames {
  J2ktiosinteropEnumNames *e;
  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

  e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_ONE);
  e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_TWO);

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;

  e = [J2ktiosinteropEnumNames valueOfWithNSString:@"ONE"];
  e = [J2ktiosinteropEnumNames valueOfWithNSString:@"TWO"];

  IOSObjectArray *values = [J2ktiosinteropEnumNames values];
  e = values[0];
  e = values[1];
}

@end
