#import <XCTest/XCTest.h>

#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/ApplicationProtocol.h"

@interface ApplicationABTest : XCTestCase
@end

@implementation ApplicationABTest

- (void)testApplication {
  Class j2objcApplicationClass = NSClassFromString(@"J2ObjCApplication");
  Class j2ktApplicationClass = NSClassFromString(@"J2ktApplication");

  XCTAssertNotNil(j2objcApplicationClass);
  XCTAssertNotNil(j2ktApplicationClass);

  id<ApplicationProtocol> j2objcApplication = [[j2objcApplicationClass alloc] init];
  id<ApplicationProtocol> j2ktApplication = [[j2ktApplicationClass alloc] init];

  XCTAssertNotNil(j2objcApplication);
  XCTAssertNotNil(j2ktApplication);

  XCTAssertEqualObjects(j2objcApplication.platformName, @"J2ObjC");
  XCTAssertEqualObjects(j2ktApplication.platformName, @"J2KT");
}

@end
