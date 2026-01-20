#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/J2ObjCApplication.h"

#include "j2ktiosinterop/Platform.h"

@implementation J2ObjCApplication {
}

- (NSString *)platformName {
  return J2ktiosinteropPlatform_get_NAME();
}

@end
