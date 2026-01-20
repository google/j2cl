#import "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/J2ktApplication.h"

#include "j2ktiosinterop/Platform.h"

@implementation J2ktApplication {
}

- (NSString *)platformName {
  return J2ktiosinteropPlatform_get_NAME();
}

@end
