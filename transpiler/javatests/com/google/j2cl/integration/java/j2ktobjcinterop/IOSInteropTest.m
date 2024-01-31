#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktobjcinterop/CustomNames.h"
#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktobjcinterop/DefaultNames.h"

@interface IOSInteropTest : XCTestCase
@end

@implementation IOSInteropTest

- (void)testDefaultNames {
  J2ktJ2ktobjcinteropDefaultNames *obj;
  obj = [[J2ktJ2ktobjcinteropDefaultNames alloc] init];
  obj = [[J2ktJ2ktobjcinteropDefaultNames alloc] initWithInt:1];
  obj = [[J2ktJ2ktobjcinteropDefaultNames alloc] initWithInt:1 withNSString:@""];

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

  obj.field_ = obj.field_;
}

- (void)testCustomNames {
  J2ktCustom *obj;
  obj = [[J2ktCustom alloc] initWithIndex:1];
  obj = [[J2ktCustom alloc] initWithIndex:1 name:@""];

  obj = [[J2ktCustom alloc] init];
  obj = [[J2ktCustom alloc] initWithLong:1];
  obj = [[J2ktCustom alloc] initWithLong:1 withNSString:@""];

  [obj custom];
  [obj customWithIndex:1];
  [obj customWithIndex:1 name:@""];

  [obj customWithLong:1];
  [obj customWithLong:1 withNSString:@""];
}

@end
