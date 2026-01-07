#import <XCTest/XCTest.h>
#import <math.h>

#import "j2ktiosinterop/CollectionTypes.h"
#import "j2ktiosinterop/CompileTimeConstantInitialization.h"
#import "j2ktiosinterop/CompileTimeConstants.h"
#import "j2ktiosinterop/CustomNames.h"
#import "j2ktiosinterop/DefaultNames.h"
#import "j2ktiosinterop/EnumNames.h"
#import "j2ktiosinterop/ImmutableList.h"
#import "j2ktiosinterop/InterfaceDefaultMethod.h"
#import "j2ktiosinterop/InterfaceDefaultMethodImpl.h"
#import "j2ktiosinterop/InterfaceDefaultMethodOverrideImpl.h"
#import "j2ktiosinterop/NativeCustomName.h"
#import "j2ktiosinterop/NativeDefaultName.h"
#import "j2ktiosinterop/OnlyExplicitDefaultConstructor.h"
#import "j2ktiosinterop/OnlyImplicitDefaultConstructor.h"
#import "j2ktiosinterop/PackageNames.h"
#import "j2ktiosinterop/PropertyAutoValueClass.h"
#import "j2ktiosinterop/PropertyAutoValueMethod.h"
#import "j2ktiosinterop/PropertyClass.h"
#import "j2ktiosinterop/PropertyClassOverride.h"
#import "j2ktiosinterop/PropertyMethod.h"
#import "j2ktiosinterop/PropertyMethodOverride.h"
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

  i = J2ktiosinteropDefaultNames_get_finalIntField();
  i = obj->intField_;
  obj->intField_ = i;

  i = J2ktiosinteropDefaultNames_get_STATIC_FINAL_INT_FIELD();
  i = J2ktiosinteropDefaultNames_get_staticIntField();
  J2ktiosinteropDefaultNames_set_staticIntField(i);

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticIntMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticIntStringMethodWithInt_withNSString_(1, @"");

  J2ktiosinteropDefaultNames_staticGenericStringMethodWithNSString_(nil);
  J2ktiosinteropDefaultNames_staticGenericStringAndComparableStringMethodWithNSString_(nil);

  J2ktiosinteropDefaultNames_staticGenericLongMethodWithJavaLangLong_(nil);
  J2ktiosinteropDefaultNames_staticGenericLongAndComparableLongMethodWithJavaLangLong_(nil);

  // For methods that throw, J2ObjC generates variants with and without `error:` parameter
  [obj throwsMethod];
  [obj throwsMethodWithNSString:@""];
  [J2ktiosinteropDefaultNames staticThrowsMethod];
  [J2ktiosinteropDefaultNames staticThrowsMethodWithNSString:@""];

  [obj throwsMethodAndReturnError:nil];
  [obj throwsMethodWithNSString:@"" error:nil];
  [J2ktiosinteropDefaultNames staticThrowsMethodAndReturnError:nil];
  [J2ktiosinteropDefaultNames staticThrowsMethodWithNSString:@"" error:nil];
}

- (void)testPackageNames {
  J2ktiosinteropPackageNames *obj = create_J2ktiosinteropPackageNames_init();

  [obj methodWithSRCDefaultFromSrc:nil];
  [obj methodWithNewCustomFromSrc:nil];
  [obj methodWithDEPDefaultFromDep:nil];
  [obj methodWithNewCustomFromDep:nil];

  J2ktiosinteropPackageNames_staticMethodWithSRCDefaultFromSrc_(nil);
  J2ktiosinteropPackageNames_staticMethodWithNewCustomFromSrc_(nil);
  J2ktiosinteropPackageNames_staticMethodWithDEPDefaultFromDep_(nil);
  J2ktiosinteropPackageNames_staticMethodWithNewCustomFromDep_(nil);
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

- (void)testEnumComparison {
  XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() == J2ktiosinteropEnumNames_get_ONE());
  XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() != J2ktiosinteropEnumNames_get_TWO());

  XCTAssertTrue([J2ktiosinteropEnumNames_get_ONE() isEqual:J2ktiosinteropEnumNames_get_ONE()]);
  XCTAssertFalse([J2ktiosinteropEnumNames_get_ONE() isEqual:J2ktiosinteropEnumNames_get_TWO()]);

  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_ONE() compareToWithId:J2ktiosinteropEnumNames_get_ONE()], 0);
  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_ONE() compareToWithId:J2ktiosinteropEnumNames_get_TWO()], -1);
  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_TWO() compareToWithId:J2ktiosinteropEnumNames_get_ONE()], 1);
}

- (void)testNativeDefaultName {
  J2ktiosinteropNativeDefaultName *obj = [[J2ktiosinteropNativeDefaultName alloc] init];
  obj = create_J2ktiosinteropNativeDefaultName_init();
  obj = new_J2ktiosinteropNativeDefaultName_init();

  [obj nativeInstanceMethod];

  [J2ktiosinteropNativeDefaultName nativeStaticMethod];
  [J2ktiosinteropNativeDefaultName nativeParameterWithJ2ktiosinteropNativeDefaultName:obj];
  [J2ktiosinteropNativeDefaultName nativeReturnType];

  J2ktiosinteropNativeDefaultName_nativeStaticMethod();
  J2ktiosinteropNativeDefaultName_nativeParameterWithJ2ktiosinteropNativeDefaultName_(obj);
  J2ktiosinteropNativeDefaultName_nativeReturnType();
}

- (void)testNativeCustomName {
  CustomNativeClass *obj = [[CustomNativeClass alloc] init];
  obj = create_CustomNativeClass_init();
  obj = new_CustomNativeClass_init();

  [obj nativeInstanceMethod];

  [CustomNativeClass nativeStaticMethod];
  [CustomNativeClass nativeParameterWithCustomNativeClass:obj];
  [CustomNativeClass nativeReturnType];

  CustomNativeClass_nativeStaticMethod();
  CustomNativeClass_nativeParameterWithCustomNativeClass_(obj);
  CustomNativeClass_nativeReturnType();
}

- (void)testCollectionTypes {
  id<JavaUtilIterator> iterator = [J2ktiosinteropCollectionTypes getIterator];
  J2ktiosinteropCollectionTypes_CustomIterator *customIterator =
      [J2ktiosinteropCollectionTypes getCustomIterator];

  J2ktiosinteropCollectionTypes_CustomIterator_Builder *customIteratorBuilder =
      [J2ktiosinteropCollectionTypes_CustomIterator builder];
  customIterator = [customIteratorBuilder build];

  id<JavaUtilListIterator> listIterator = [J2ktiosinteropCollectionTypes getListIterator];
  J2ktiosinteropCollectionTypes_CustomListIterator *customListIterator =
      [J2ktiosinteropCollectionTypes getCustomListIterator];

  J2ktiosinteropCollectionTypes_CustomListIterator_Builder *customListIteratorBuilder =
      [J2ktiosinteropCollectionTypes_CustomListIterator builder];
  customListIterator = [customListIteratorBuilder build];

  id<JavaLangIterable> iterable = [J2ktiosinteropCollectionTypes getIterable];
  J2ktiosinteropCollectionTypes_CustomIterable *customIterable =
      [J2ktiosinteropCollectionTypes getCustomIterable];

  J2ktiosinteropCollectionTypes_CustomIterable_Builder *customIterableBuilder =
      [J2ktiosinteropCollectionTypes_CustomIterable builder];
  customIterable = [customIterableBuilder build];

  id<JavaUtilCollection> collection = [J2ktiosinteropCollectionTypes getCollection];
  JavaUtilAbstractCollection *abstractCollection =
      [J2ktiosinteropCollectionTypes getAbstractCollection];
  J2ktiosinteropCollectionTypes_CustomCollection *customCollection =
      [J2ktiosinteropCollectionTypes getCustomCollection];

  J2ktiosinteropCollectionTypes_CustomCollection_Builder *customCollectionBuilder =
      [J2ktiosinteropCollectionTypes_CustomCollection builder];
  customCollection = [customCollectionBuilder build];

  id<JavaUtilList> list = [J2ktiosinteropCollectionTypes getList];
  JavaUtilArrayList *arraylist = [J2ktiosinteropCollectionTypes getArrayList];
  JavaUtilLinkedList *linkedList = [J2ktiosinteropCollectionTypes getLinkedList];
  JavaUtilAbstractList *abstractList = [J2ktiosinteropCollectionTypes getAbstractList];
  J2ktiosinteropCollectionTypes_CustomList *customList =
      [J2ktiosinteropCollectionTypes getCustomList];

  J2ktiosinteropCollectionTypes_CustomList_Builder *customListBuilder =
      [J2ktiosinteropCollectionTypes_CustomList builder];
  customList = [customListBuilder build];

  id<JavaUtilSet> set = [J2ktiosinteropCollectionTypes getSet];
  JavaUtilHashSet *hashSet = [J2ktiosinteropCollectionTypes getHashSet];
  JavaUtilAbstractSet *abstractSet = [J2ktiosinteropCollectionTypes getAbstractSet];
  J2ktiosinteropCollectionTypes_CustomSet *customSet = [J2ktiosinteropCollectionTypes getCustomSet];

  J2ktiosinteropCollectionTypes_CustomSet_Builder *customSetBuilder =
      [J2ktiosinteropCollectionTypes_CustomSet builder];
  customSet = [customSetBuilder build];

  id<JavaUtilMap> map = [J2ktiosinteropCollectionTypes getMap];
  JavaUtilHashMap *hashMap = [J2ktiosinteropCollectionTypes getHashMap];
  JavaUtilLinkedHashMap *linkedHashMap = [J2ktiosinteropCollectionTypes getLinkedHashMap];
  JavaUtilAbstractMap *abstractMap = [J2ktiosinteropCollectionTypes getAbstractMap];
  J2ktiosinteropCollectionTypes_CustomMap *customMap = [J2ktiosinteropCollectionTypes getCustomMap];

  J2ktiosinteropCollectionTypes_CustomMap_Builder *customMapBuilder =
      [J2ktiosinteropCollectionTypes_CustomMap builder];
  customMap = [customMapBuilder build];

  [J2ktiosinteropCollectionTypes acceptIteratorWithJavaUtilIterator:iterator];
  [J2ktiosinteropCollectionTypes
      acceptCustomIteratorWithJ2ktiosinteropCollectionTypes_CustomIterator:customIterator];

  [J2ktiosinteropCollectionTypes acceptListIteratorWithJavaUtilListIterator:listIterator];
  [J2ktiosinteropCollectionTypes
      acceptCustomListIteratorWithJ2ktiosinteropCollectionTypes_CustomListIterator:
          customListIterator];

  [J2ktiosinteropCollectionTypes acceptIterableWithJavaLangIterable:iterable];
  [J2ktiosinteropCollectionTypes
      acceptCustomIterableWithJ2ktiosinteropCollectionTypes_CustomIterable:customIterable];

  [J2ktiosinteropCollectionTypes acceptCollectionWithJavaUtilCollection:collection];
  [J2ktiosinteropCollectionTypes
      acceptAbstractCollectionWithJavaUtilAbstractCollection:abstractCollection];
  [J2ktiosinteropCollectionTypes
      acceptCustomCollectionWithJ2ktiosinteropCollectionTypes_CustomCollection:customCollection];

  [J2ktiosinteropCollectionTypes acceptListWithJavaUtilList:list];
  [J2ktiosinteropCollectionTypes acceptArrayListWithJavaUtilArrayList:arraylist];
  [J2ktiosinteropCollectionTypes acceptLinkedListWithJavaUtilLinkedList:linkedList];
  [J2ktiosinteropCollectionTypes acceptAbstractListWithJavaUtilAbstractList:abstractList];
  [J2ktiosinteropCollectionTypes
      acceptCustomListWithJ2ktiosinteropCollectionTypes_CustomList:customList];

  [J2ktiosinteropCollectionTypes acceptSetWithJavaUtilSet:set];
  [J2ktiosinteropCollectionTypes acceptHashSetWithJavaUtilHashSet:hashSet];
  [J2ktiosinteropCollectionTypes acceptAbstractSetWithJavaUtilAbstractSet:abstractSet];
  [J2ktiosinteropCollectionTypes
      acceptCustomSetWithJ2ktiosinteropCollectionTypes_CustomSet:customSet];

  [J2ktiosinteropCollectionTypes acceptMapWithJavaUtilMap:map];
  [J2ktiosinteropCollectionTypes acceptHashMapWithJavaUtilHashMap:hashMap];
  [J2ktiosinteropCollectionTypes acceptLinkedHashMapWithJavaUtilLinkedHashMap:linkedHashMap];
  [J2ktiosinteropCollectionTypes acceptAbstractMapWithJavaUtilAbstractMap:abstractMap];
  [J2ktiosinteropCollectionTypes
      acceptCustomMapWithJ2ktiosinteropCollectionTypes_CustomMap:customMap];
}

- (void)testImmutableList {
  J2ktiosinteropImmutableList *list;
  list = J2ktiosinteropImmutableList_of();
  list = J2ktiosinteropImmutableList_ofWithId_(@"foo");
  list = J2ktiosinteropImmutableList_ofWithId_withId_(@"foo", @"bar");
  list = J2ktiosinteropImmutableList_copyOfWithJavaLangIterable_(list);

  J2ktiosinteropImmutableList_Builder *builder;

  builder = J2ktiosinteropImmutableList_builder();
  [builder addWithId:@"foo"];
  [builder addWithId:@"bar"];
  list = [builder build];

  builder = [J2ktiosinteropImmutableList builder];
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
  throwable = [[JavaLangThrowable alloc] initWithNSString:@"foo"];
}

- (void)testInterface {
  id<J2ktiosinteropTestInterface> testInterface = [[TestImplementation alloc] init];
  [testInterface testMethod];
  XCTAssertTrue([testInterface isKindOfClass:[TestImplementation class]]);
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

  XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_CONSTANT_STRING, @"foo");
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());

  XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_get_CONSTANT_STRING(), @"foo");
  // Surprisingly, it causes class initialiation.
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

- (void)testInterfaceDefaultMethod {
  J2ktiosinteropInterfaceDefaultMethodImpl *interfaceDefaultMethod =
      create_J2ktiosinteropInterfaceDefaultMethodImpl_init();
  XCTAssertEqual([interfaceDefaultMethod getInt], 1);
  XCTAssertEqual([interfaceDefaultMethod getDefaultLong], 1);
  XCTAssertEqual([interfaceDefaultMethod getSuperDefaultLong], 1);
  // This function is designed to support `super` calls to default methods, but for the purpose of
  // this test can be used as a regular method.
  XCTAssertEqual(J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethod), 1);

  J2ktiosinteropInterfaceDefaultMethodOverrideImpl *interfaceDefaultMethodOverride =
      create_J2ktiosinteropInterfaceDefaultMethodOverrideImpl_init();
  XCTAssertEqual([interfaceDefaultMethodOverride getInt], 1);
  XCTAssertEqual([interfaceDefaultMethodOverride getDefaultLong], 2);
  XCTAssertEqual([interfaceDefaultMethodOverride getSuperDefaultLong], 1);
  // This function is designed to support `super` calls to default methods, but for the purpose of
  // this test can be used as a regular method.
  XCTAssertEqual(
      J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethodOverride), 1);
}

- (void)testProperties {
  J2ktiosinteropPropertyClass *propertyClass = create_J2ktiosinteropPropertyClass_init();
  XCTAssertEqual(propertyClass.intMethod, 0);
  XCTAssertEqual([propertyClass intSuppressMethod], 0);
  XCTAssertEqual([propertyClass intMethodWithParamWithInt:1], 1);
  XCTAssertEqual(propertyClass.longMethod, 0);

  J2ktiosinteropPropertyMethod *propertyMethod = create_J2ktiosinteropPropertyMethod_init();
  XCTAssertEqual(propertyMethod.intMethod, 0);
  XCTAssertEqual(propertyMethod.longMethod, 0);

  J2ktiosinteropPropertyClassOverride *propertyClassOverride =
      create_J2ktiosinteropPropertyClassOverride_init();
  XCTAssertEqual(propertyClassOverride.intMethod, 0);
  XCTAssertEqual([propertyClassOverride intSuppressMethod], 0);
  XCTAssertEqual([propertyClassOverride intMethodWithParamWithInt:1], 1);
  XCTAssertEqual(propertyClassOverride.longMethod, 0);
  XCTAssertEqual([propertyClassOverride nonOverrideIntMethod], 0);

  J2ktiosinteropPropertyMethodOverride *propertyMethodOverride =
      create_J2ktiosinteropPropertyMethodOverride_init();
  XCTAssertEqual(propertyMethodOverride.intMethod, 0);
  XCTAssertEqual(propertyMethodOverride.longMethod, 0);
  XCTAssertEqual([propertyMethodOverride nonOverrideIntMethod], 0);

  J2ktiosinteropPropertyAutoValueClass *propertyAutoValueClass =
      [[J2ktiosinteropPropertyAutoValueClass_builder() setNameWithNSString:@"foo"] build];
  XCTAssertEqual(propertyAutoValueClass.name, @"foo");

  J2ktiosinteropPropertyAutoValueMethod *propertyAutoValueMethod =
      [[J2ktiosinteropPropertyAutoValueMethod_builder() setNameWithNSString:@"foo"] build];
  XCTAssertEqual(propertyAutoValueMethod.name, @"foo");
}

@end
