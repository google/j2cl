#import <XCTest/XCTest.h>
#import <math.h>

#import "j2ktiosinterop/CollectionTypes.h"
#import "j2ktiosinterop/CompileTimeConstantInitialization.h"
#import "j2ktiosinterop/CompileTimeConstants.h"
#import "j2ktiosinterop/CustomNames.h"
#import "j2ktiosinterop/DefaultNames.h"
#import "j2ktiosinterop/EnumNames.h"
#import "j2ktiosinterop/ImmutableList.h"
#import "j2ktiosinterop/NativeCustomName.h"
#import "j2ktiosinterop/NativeDefaultName.h"
#import "j2ktiosinterop/OnlyExplicitDefaultConstructor.h"
#import "j2ktiosinterop/OnlyImplicitDefaultConstructor.h"
#import "j2ktiosinterop/SpecialNames.h"
#import "j2ktiosinterop/TestInterface.h"
#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "java/lang/Integer.h"
#include "java/lang/Throwable.h"

@interface TestImplementation : NSObject <J2ktiosinteropTestInterface>
@end

@implementation TestImplementation
- (void)testMethod {
}
@end

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
  [obj genericStringMethodWithNSString:nil];
  [obj genericStringAndComparableStringMethodWithNSString:nil];
  [obj genericLongMethodWithJavaLangLong:nil];
  [obj genericLongAndComparableLongMethodWithJavaLangLong:nil];

  [obj overloadedMethodWithId:nil];
  [obj overloadedMethodWithInt:1];
  [obj overloadedMethodWithLong:1];

  [obj overloadedMethodWithFloat:1];
  [obj overloadedMethodWithDouble:1];
  [obj overloadedMethodWithNSString:@""];

  int i;

  i = obj.finalIntField_;
  i = obj.intField_;
  obj.intField_ = i;

  i = J2ktJ2ktiosinteropDefaultNamesCompanion.shared.STATIC_FINAL_INT_FIELD;
  i = J2ktJ2ktiosinteropDefaultNamesCompanion.shared.staticIntField;
  J2ktJ2ktiosinteropDefaultNamesCompanion.shared.staticIntField = i;

  i = J2ktiosinteropDefaultNames_get_STATIC_FINAL_INT_FIELD();
  i = J2ktiosinteropDefaultNames_get_staticIntField();
  J2ktiosinteropDefaultNames_set_staticIntField(i);

  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticMethod];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticIntMethodWithInt:1];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticIntStringMethodWithInt:1 withNSString:@""];

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticIntMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticIntStringMethodWithInt_withNSString_(1, @"");

  J2ktiosinteropDefaultNames_staticGenericStringMethodWithNSString_(nil);
  J2ktiosinteropDefaultNames_staticGenericStringAndComparableStringMethodWithNSString_(nil);

  // Not available because of JavaLangLong is not available.
  // J2ktiosinteropDefaultNames_staticGenericLongMethodWithJavaLangLong_(nil);
  // J2ktiosinteropDefaultNames_staticGenericLongAndComparableLongMethodWithJavaLangLong_(nil);

  // For methods that throw, J2KT generates `error:` parameter.
  [obj throwsMethodAndReturnError:nil];
  [obj throwsMethodWithNSString:@"" error:nil];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticThrowsMethodAndReturnError:nil];
  [J2ktJ2ktiosinteropDefaultNamesCompanion.shared staticThrowsMethodWithNSString:@"" error:nil];
}

- (void)testOnlyImplicitDefaultConstructor {
  J2ktJ2ktiosinteropOnlyImplicitDefaultConstructor *obj;
  obj = [[J2ktJ2ktiosinteropOnlyImplicitDefaultConstructor alloc] init];
  obj = create_J2ktiosinteropOnlyImplicitDefaultConstructor_init();
  obj = new_J2ktiosinteropOnlyImplicitDefaultConstructor_init();
}

- (void)testOnlyExplicitDefaultConstructor {
  J2ktJ2ktiosinteropOnlyExplicitDefaultConstructor *obj;
  obj = [[J2ktJ2ktiosinteropOnlyExplicitDefaultConstructor alloc] init];
  obj = create_J2ktiosinteropOnlyExplicitDefaultConstructor_init();
  obj = new_J2ktiosinteropOnlyExplicitDefaultConstructor_init();
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
  [[[J2ktJ2ktiosinteropSpecialNames_WithObject alloc] init] getWithId:nil];
  [[[J2ktJ2ktiosinteropSpecialNames_WithString alloc] init] getWithNSString:@""];
  [[[J2ktJ2ktiosinteropSpecialNames_WithFoo alloc] init] getWithJ2ktiosinteropSpecialNames_Foo:nil];
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

  [obj customCustomNamesMethodWithCustom:nil];
  [obj customDefaultNamesMethodWithJ2ktiosinteropDefaultNames:nil];

  [obj customObjectiveCSwiftStringMethodWithString:@""];
  [obj swiftStringMethodWithNSString:@""];

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

  // TODO(b/441689301): Unsupported because of https://youtrack.jetbrains.com/issue/KT-80557
  // [obj lowercase:@""];
  // Custom_staticlowercase_(@"");
}

- (void)testEnumNames {
  J2ktJ2ktiosinteropEnumNames *e;
  e = J2ktJ2ktiosinteropEnumNames.ONE;
  e = J2ktJ2ktiosinteropEnumNames.TWO;

  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

  // Not supported
  // e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_ONE);
  // e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_TWO);

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;

  // Not supported
  // e = [J2ktiosinteropEnumNames valueOfWithNSString:@"ONE"];
  // e = [J2ktiosinteropEnumNames valueOfWithNSString:@"TWO"];

  GKOTKotlinArray<J2ktJ2ktiosinteropEnumNames *> *values = [J2ktiosinteropEnumNames values];
  e = [values getIndex:0];
  e = [values getIndex:1];
}

- (void)testNativeDefaultName {
  J2ktiosinteropNativeDefaultName *obj = [[J2ktiosinteropNativeDefaultName alloc] init];
  obj = create_J2ktiosinteropNativeDefaultName_init();
  obj = new_J2ktiosinteropNativeDefaultName_init();

  [obj nativeInstanceMethod];

  [J2ktiosinteropNativeDefaultNameCompanion.shared nativeStaticMethod];
  [J2ktiosinteropNativeDefaultNameCompanion.shared
      nativeParameterWithJ2ktiosinteropNativeDefaultName:obj];
  [J2ktiosinteropNativeDefaultNameCompanion.shared nativeReturnType];

  J2ktiosinteropNativeDefaultName_nativeStaticMethod();
  J2ktiosinteropNativeDefaultName_nativeParameterWithJ2ktiosinteropNativeDefaultName_(obj);
  J2ktiosinteropNativeDefaultName_nativeReturnType();
}

- (void)testNativeCustomName {
  CustomNativeClass *obj = [[CustomNativeClass alloc] init];
  obj = create_CustomNativeClass_init();
  obj = new_CustomNativeClass_init();

  [obj nativeInstanceMethod];

  [CustomNativeClassCompanion.shared nativeStaticMethod];
  [CustomNativeClassCompanion.shared nativeParameterWithCustomNativeClass:obj];
  [CustomNativeClassCompanion.shared nativeReturnType];

  CustomNativeClass_nativeStaticMethod();
  CustomNativeClass_nativeParameterWithCustomNativeClass_(obj);
  CustomNativeClass_nativeReturnType();
}

- (void)testCollectionTypes {
  id<GKOTKotlinMutableIterator> iterator =
      [J2ktiosinteropCollectionTypesCompanion.shared getIterator];
  J2ktiosinteropCollectionTypes_CustomIterator *customIterator =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomIterator];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomIterator_Builder *customIteratorBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomIteratorCompanion.shared builder];
  // customIterator = [customIteratorBuilder build];

  id<GKOTKotlinMutableListIterator> listIterator =
      [J2ktiosinteropCollectionTypesCompanion.shared getListIterator];
  J2ktiosinteropCollectionTypes_CustomListIterator *customListIterator =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomListIterator];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomListIterator_Builder *customListIteratorBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomListIteratorCompanion.shared builder];
  // customListIterator = [customListIteratorBuilder build];

  id iterable = [J2ktiosinteropCollectionTypesCompanion.shared getIterable];
  J2ktiosinteropCollectionTypes_CustomIterable *customIterable =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomIterable];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomIterable_Builder *customIterableBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomIterableCompanion.shared builder];
  // customIterable = [customIterableBuilder build];

  id collection = [J2ktiosinteropCollectionTypesCompanion.shared getCollection];
  J2ktJavaUtilAbstractCollection<id> *abstractCollection =
      [J2ktiosinteropCollectionTypesCompanion.shared getAbstractCollection];
  J2ktJ2ktiosinteropCollectionTypes_CustomCollection<id> *customCollection =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomCollection];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomCollection_Builder *customCollectionBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomCollectionCompanion.shared builder];
  // customCollection = [customCollectionBuilder build];

  NSMutableArray<id> *list = [J2ktiosinteropCollectionTypesCompanion.shared getList];
  NSMutableArray<id> *arraylist = [J2ktiosinteropCollectionTypesCompanion.shared getArrayList];
  NSMutableArray<id> *linkedList = [J2ktiosinteropCollectionTypesCompanion.shared getLinkedList];
  NSMutableArray<id> *abstractList =
      [J2ktiosinteropCollectionTypesCompanion.shared getAbstractList];
  NSMutableArray<id> *customList = [J2ktiosinteropCollectionTypesCompanion.shared getCustomList];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomList_Builder *customListBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomListCompanion.shared builder];
  // customList = [customListBuilder build];

  GKOTMutableSet<id> *set = [J2ktiosinteropCollectionTypesCompanion.shared getSet];
  GKOTMutableSet<id> *hashSet = [J2ktiosinteropCollectionTypesCompanion.shared getHashSet];
  GKOTMutableSet<id> *abstractSet = [J2ktiosinteropCollectionTypesCompanion.shared getAbstractSet];
  GKOTMutableSet<id> *customSet = [J2ktiosinteropCollectionTypesCompanion.shared getCustomSet];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomSet_Builder *customSetBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomSetCompanion.shared builder];
  // customSet = [customSetBuilder build];

  GKOTMutableDictionary<id, id> *map = [J2ktiosinteropCollectionTypesCompanion.shared getMap];
  GKOTMutableDictionary<id, id> *hashMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getHashMap];
  GKOTMutableDictionary<id, id> *linkedHashMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getLinkedHashMap];
  GKOTMutableDictionary<id, id> *abstractMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getAbstractMap];
  GKOTMutableDictionary<id, id> *customMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomMap];

  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptIteratorWithKotlinCollectionsMutableIterator:iterator];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomIteratorWithJ2ktiosinteropCollectionTypes_CustomIterator:customIterator];

  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptListIteratorWithKotlinCollectionsMutableListIterator:listIterator];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomListIteratorWithJ2ktiosinteropCollectionTypes_CustomListIterator:
          customListIterator];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptIterableWithJavaLangIterable:iterable];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomIterableWithJ2ktiosinteropCollectionTypes_CustomIterable:customIterable];

  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCollectionWithKotlinCollectionsMutableCollection:collection];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptAbstractCollectionWithJavaUtilAbstractCollection:abstractCollection];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomCollectionWithJ2ktiosinteropCollectionTypes_CustomCollection:customCollection];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptListWithJavaUtilList:list];
  [J2ktiosinteropCollectionTypesCompanion.shared acceptArrayListWithJavaUtilArrayList:arraylist];
  [J2ktiosinteropCollectionTypesCompanion.shared acceptLinkedListWithJavaUtilLinkedList:linkedList];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptAbstractListWithJavaUtilAbstractList:abstractList];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomListWithJ2ktiosinteropCollectionTypes_CustomList:customList];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptSetWithKotlinCollectionsMutableSet:set];
  [J2ktiosinteropCollectionTypesCompanion.shared acceptHashSetWithKotlinCollectionsHashSet:hashSet];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptAbstractSetWithJavaUtilAbstractSet:abstractSet];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomSetWithJ2ktiosinteropCollectionTypes_CustomSet:customSet];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptMapWithJavaUtilMap:map];
  [J2ktiosinteropCollectionTypesCompanion.shared acceptHashMapWithJavaUtilHashMap:hashMap];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptLinkedHashMapWithJavaUtilLinkedHashMap:linkedHashMap];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptAbstractMapWithJavaUtilAbstractMap:abstractMap];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomMapWithJ2ktiosinteropCollectionTypes_CustomMap:customMap];
}

- (void)testImmutableList {
  NSMutableArray *list;

  // TODO(b/443300128): Generate J2ObjCCompat.h for collection classes.
  // list = J2ktiosinteropImmutableList_of();
  // list = J2ktiosinteropImmutableList_ofWithId_(@"foo");
  // list = J2ktiosinteropImmutableList_ofWithId_withId_(@"foo", @"bar");
  // list = J2ktiosinteropImmutableList_copyOfWithJavaLangIterable_(list);

  // J2ktiosinteropImmutableList_Builder *builder;
  // builder = J2ktiosinteropImmutableList_builder();
  // [builder addWithId:@"foo"];
  // [builder addWithId:@"bar"];
  // list = [builder build];

  // TODO(b/443300128): Generate alias without J2kt prefix in J2ObjCCompat.h.
  J2ktJ2ktiosinteropImmutableList_Builder *builder =
      [J2ktJ2ktiosinteropImmutableListCompanion.shared builder];
  [builder addWithId:@"foo"];
  [builder addWithId:@"bar"];
  list = [builder build];
}

- (void)testPrimitiveConstants {
  int i;
  i = JavaLangInteger_get_MAX_VALUE();
  i = JavaLangInteger_get_MIN_VALUE();
}

- (void)testThrowable {
  JavaLangThrowable *throwable;
  throwable = create_JavaLangThrowable_initWithNSString_(@"foo");
  throwable = [[JavaLangThrowable alloc] initWithMessage:@"foo"];
}

- (void)testInterface {
  id<J2ktiosinteropTestInterface> testInterface = [[TestImplementation alloc] init];
  [testInterface testMethod];

  // TODO(b/284891929): Transpiled interfaces do not conform to NSObject protocol...
  // XCTAssertTrue([testInterface isKindOfClass:[TestImplementation class]]);

  // ...so explicit cast to id<NSObject> is required.
  XCTAssertTrue([((id<NSObject>)testInterface) isKindOfClass:[TestImplementation class]]);
}

- (void)testCompileTimeConstants {
  XCTAssertTrue(J2ktiosinteropCompileTimeConstants_CONSTANT_BOOLEAN);
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  XCTAssertTrue(J2ktiosinteropCompileTimeConstants_get_CONSTANT_BOOLEAN());
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  XCTAssertEqual(J2ktiosinteropCompileTimeConstants_CONSTANT_INT, 5);
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  XCTAssertEqual(J2ktiosinteropCompileTimeConstants_get_CONSTANT_INT(), 5);
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  // J2ktiosinteropCompileTimeConstants_CONSTANT_STRING is absent in Compat.h.
  // XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_CONSTANT_STRING, @"foo");
  // TODO(b/458647857): Should be false and not cause class initialization.
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_get_CONSTANT_STRING(), @"foo");
  // Surprisingly, it causes class initialiation in J2ObjC.
  XCTAssertTrue(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());
}

- (void)testSpecialPrimitiveConstants {
  XCTAssertTrue(isnan(JavaLangFloat_NaN));
  XCTAssertTrue(isinf(JavaLangFloat_NEGATIVE_INFINITY));
  XCTAssertTrue(JavaLangFloat_NEGATIVE_INFINITY < 0);
  XCTAssertTrue(isinf(JavaLangFloat_POSITIVE_INFINITY));
  XCTAssertTrue(JavaLangFloat_POSITIVE_INFINITY > 0);
  XCTAssertEqual(JavaLangFloat_MAX_VALUE, FLT_MAX);

  XCTAssertTrue(isnan(JavaLangDouble_NaN));
  XCTAssertTrue(isinf(JavaLangDouble_NEGATIVE_INFINITY));
  XCTAssertTrue(JavaLangDouble_NEGATIVE_INFINITY < 0);
  XCTAssertTrue(isinf(JavaLangDouble_POSITIVE_INFINITY));
  XCTAssertTrue(JavaLangDouble_POSITIVE_INFINITY > 0);
  XCTAssertEqual(JavaLangDouble_MAX_VALUE, DBL_MAX);
}

@end
