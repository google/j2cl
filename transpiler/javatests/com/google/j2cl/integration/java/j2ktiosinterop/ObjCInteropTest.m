#import <XCTest/XCTest.h>
#import <math.h>
#import "j2ktiosinterop/CollectionTypes.h"
#import "j2ktiosinterop/CompileTimeConstantInitialization.h"
#import "j2ktiosinterop/CompileTimeConstants.h"
#import "j2ktiosinterop/CustomNames.h"
#import "j2ktiosinterop/DataClassRecord.h"
#import "j2ktiosinterop/DefaultNames.h"
#import "j2ktiosinterop/EnumNames.h"
#import "j2ktiosinterop/ImmutableList.h"
#import "j2ktiosinterop/InterfaceDefaultMethod.h"
#import "j2ktiosinterop/InterfaceDefaultMethodImpl.h"
#import "j2ktiosinterop/InterfaceDefaultMethodOverrideImpl.h"
#import "j2ktiosinterop/NativeCustomName.h"
#import "j2ktiosinterop/NativeDefaultName.h"
#import "j2ktiosinterop/Nullability.h"
#import "j2ktiosinterop/ObjectiveCNameOverrides.h"
#import "j2ktiosinterop/OnlyExplicitDefaultConstructor.h"
#import "j2ktiosinterop/OnlyImplicitDefaultConstructor.h"
#import "j2ktiosinterop/PackageNames.h"
#import "j2ktiosinterop/Platform.h"
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

@interface ObjCInteropTest : XCTestCase
@end

@implementation ObjCInteropTest

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

#if J2KT
  i = obj.finalIntField_;
  i = obj.intField_;
  obj.intField_ = i;

  i = J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_;
  J2ktiosinteropDefaultNamesCompanion.shared.staticIntField_ = i;
#else
  // TODO(b/543364803): How does this work at all without a reference to obj?
  i = J2ktiosinteropDefaultNames_get_finalIntField();
  // Note the arrow syntax vs. dots above.
  i = obj->intField_;
  obj->intField_ = i;
#endif

  i = J2ktiosinteropDefaultNames.companion.STATIC_FINAL_INT_FIELD;

  i = J2ktiosinteropDefaultNames_get_STATIC_FINAL_INT_FIELD();
  i = J2ktiosinteropDefaultNames_get_staticIntField();
  J2ktiosinteropDefaultNames_set_staticIntField(i);

  [J2ktiosinteropDefaultNames.companion staticMethod];
  [J2ktiosinteropDefaultNames.companion staticIntMethodWithInt:1];
  [J2ktiosinteropDefaultNames.companion staticIntStringMethodWithInt:1 withNSString:@""];

  J2ktiosinteropDefaultNames_staticMethod();
  J2ktiosinteropDefaultNames_staticIntMethodWithInt_(1);
  J2ktiosinteropDefaultNames_staticIntStringMethodWithInt_withNSString_(1, @"");

  J2ktiosinteropDefaultNames_staticGenericStringMethodWithNSString_(nil);
  J2ktiosinteropDefaultNames_staticGenericStringAndComparableStringMethodWithNSString_(nil);

  J2ktiosinteropDefaultNames_staticGenericLongMethodWithJavaLangLong_(nil);
  J2ktiosinteropDefaultNames_staticGenericLongAndComparableLongMethodWithJavaLangLong_(nil);

#if !J2KT
  // For methods that throw, J2ObjC generates variants with and without `error:` parameter
  [obj throwsMethod];
  [obj throwsMethodWithNSString:@""];
  [J2ktiosinteropDefaultNames staticThrowsMethod];
  [J2ktiosinteropDefaultNames staticThrowsMethodWithNSString:@""];
#endif

  // For methods that throw, J2KT generates `error:` parameter.
  [obj throwsMethodAndReturnError:nil];
  [obj throwsMethodWithNSString:@"" error:nil];
#if J2KT
  [J2ktiosinteropDefaultNamesCompanion.shared staticThrowsMethodAndReturnError:nil];
  [J2ktiosinteropDefaultNamesCompanion.shared staticThrowsMethodWithNSString:@"" error:nil];
#else
  [J2ktiosinteropDefaultNames staticThrowsMethodAndReturnError:nil];
  [J2ktiosinteropDefaultNames staticThrowsMethodWithNSString:@"" error:nil];
#endif
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
#if J2KT
  // TODO(b/400390599): Should be `init2WithLong:1`
  obj = [[Custom alloc] initWithLong:1];
  // TODO(b/400390599): Should be `init3WithLong:withNSString`
  obj = [[Custom alloc] initWithLong:1 withNSString:@""];
#else
  obj = [[Custom alloc] init2WithLong:1];
  obj = [[Custom alloc] init3WithLong:1 withNSString:@""];
#endif

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

  [obj swiftStringMethodWithNSString:@""];
  [obj customObjectiveCStringMethodWithString:@""];

  [Custom.companion customStaticMethod];
  [Custom.companion customStaticIntMethodWithIndex:1];
  [Custom.companion customStaticIntStringMethodWithIndex:1 name:@""];

  [Custom.companion customStaticLongMethodWithLong:1];
  [Custom.companion customStaticLongStringMethodWithLong:1 withNSString:@""];

  Custom_customStaticMethod();
  Custom_customStaticIntMethodWithIndex_(1);
  Custom_customStaticIntStringMethodWithIndex_name_(1, @"");

  Custom_customStaticLongMethod(1);
  Custom_customStaticLongStringMethod(2, @"");

  [obj lowercase:@""];
  Custom_staticlowercase_(@"");

  [Custom.companion eventApiColorIndexFor:@""];
  Custom_eventApiColorIndexFor_(@"");
}

- (void)testEnumNames {
  J2ktiosinteropEnumNames *e;

  // Recommended
  e = J2ktiosinteropEnumNames.ONE;
  e = J2ktiosinteropEnumNames.TWO;

  // Supported for compatibility
  e = J2ktiosinteropEnumNames_get_ONE();
  e = J2ktiosinteropEnumNames_get_TWO();

#if !J2KT
  e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_ONE);
  e = J2ktiosinteropEnumNames_fromOrdinal(J2ktiosinteropEnumNames_Enum_TWO);
#endif

  J2ktiosinteropEnumNames_Enum e2;
  e2 = J2ktiosinteropEnumNames_Enum_ONE;
  e2 = J2ktiosinteropEnumNames_Enum_TWO;

#if !J2KT
  e = [J2ktiosinteropEnumNames valueOfWithNSString:@"ONE"];
  e = [J2ktiosinteropEnumNames valueOfWithNSString:@"TWO"];

  IOSObjectArray *values = [J2ktiosinteropEnumNames values];
  e = values[0];
  e = values[1];
#else
  GKOTKotlinArray<J2ktiosinteropEnumNames *> *values = [J2ktiosinteropEnumNames values];
  e = [values getIndex:0];
  e = [values getIndex:1];
#endif
}

- (void)testEnumComparison {
  XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() == J2ktiosinteropEnumNames_get_ONE());
  XCTAssertTrue(J2ktiosinteropEnumNames_get_ONE() != J2ktiosinteropEnumNames_get_TWO());

  XCTAssertTrue([J2ktiosinteropEnumNames_get_ONE() isEqual:J2ktiosinteropEnumNames_get_ONE()]);
  XCTAssertFalse([J2ktiosinteropEnumNames_get_ONE() isEqual:J2ktiosinteropEnumNames_get_TWO()]);

#if !J2KT
  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_ONE() compareToWithId:J2ktiosinteropEnumNames_get_ONE()], 0);
  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_ONE() compareToWithId:J2ktiosinteropEnumNames_get_TWO()], -1);
  XCTAssertEqual(
      [J2ktiosinteropEnumNames_get_TWO() compareToWithId:J2ktiosinteropEnumNames_get_ONE()], 1);
#endif
}

- (void)testNativeDefaultName {
  J2ktiosinteropNativeDefaultName *obj = [[J2ktiosinteropNativeDefaultName alloc] init];
  obj = create_J2ktiosinteropNativeDefaultName_init();
  obj = new_J2ktiosinteropNativeDefaultName_init();

  [obj nativeInstanceMethod];

  [J2ktiosinteropNativeDefaultName.companion nativeStaticMethod];
  [J2ktiosinteropNativeDefaultName.companion
      nativeParameterWithJ2ktiosinteropNativeDefaultName:obj];
  [J2ktiosinteropNativeDefaultName.companion nativeReturnType];

  J2ktiosinteropNativeDefaultName_nativeStaticMethod();
  J2ktiosinteropNativeDefaultName_nativeParameterWithJ2ktiosinteropNativeDefaultName_(obj);
  J2ktiosinteropNativeDefaultName_nativeReturnType();

#if !J2KT
  // Not planned for J2kt
  [J2ktiosinteropNativeDefaultName nativeStaticMethod];
  [J2ktiosinteropNativeDefaultName nativeParameterWithJ2ktiosinteropNativeDefaultName:obj];
  [J2ktiosinteropNativeDefaultName nativeReturnType];
#endif
}

- (void)testNativeCustomName {
  CustomNativeClass *obj = [[CustomNativeClass alloc] init];
  obj = create_CustomNativeClass_init();
  obj = new_CustomNativeClass_init();

  [obj nativeInstanceMethod];

  [CustomNativeClass.companion nativeStaticMethod];
  [CustomNativeClass.companion nativeParameterWithCustomNativeClass:obj];
  [CustomNativeClass.companion nativeReturnType];

#if !J2KT
  // Not planned for J2kt
  [CustomNativeClass nativeStaticMethod];
  [CustomNativeClass nativeParameterWithCustomNativeClass:obj];
  [CustomNativeClass nativeReturnType];
#endif

  CustomNativeClass_nativeStaticMethod();
  CustomNativeClass_nativeParameterWithCustomNativeClass_(obj);
  CustomNativeClass_nativeReturnType();
}

- (void)testCollectionTypes {
#if J2KT
  id<GKOTKotlinIterator> iterator = [J2ktiosinteropCollectionTypesCompanion.shared getIterator];
  J2ktiosinteropCollectionTypes_CustomIterator *customIterator =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomIterator];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomIterator_Builder *customIteratorBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomIteratorCompanion.shared builder];
  // customIterator = [customIteratorBuilder build];

  id<GKOTKotlinListIterator> listIterator =
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

  NSArray<id> *list = [J2ktiosinteropCollectionTypesCompanion.shared getList];
  NSMutableArray<id> *arraylist = [J2ktiosinteropCollectionTypesCompanion.shared getArrayList];
  NSMutableArray<id> *linkedList = [J2ktiosinteropCollectionTypesCompanion.shared getLinkedList];
  NSMutableArray<id> *abstractList =
      [J2ktiosinteropCollectionTypesCompanion.shared getAbstractList];
  NSMutableArray<id> *customList = [J2ktiosinteropCollectionTypesCompanion.shared getCustomList];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomList_Builder *customListBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomListCompanion.shared builder];
  // customList = [customListBuilder build];

  NSSet<id> *set = [J2ktiosinteropCollectionTypesCompanion.shared getSet];
  GKOTMutableSet<id> *hashSet = [J2ktiosinteropCollectionTypesCompanion.shared getHashSet];
  GKOTMutableSet<id> *abstractSet = [J2ktiosinteropCollectionTypesCompanion.shared getAbstractSet];
  GKOTMutableSet<id> *customSet = [J2ktiosinteropCollectionTypesCompanion.shared getCustomSet];

  // TODO(b/454834286): Generate J2ObjCCompat.h for inner collection classes.
  // J2ktiosinteropCollectionTypes_CustomSet_Builder *customSetBuilder =
  //     [J2ktiosinteropCollectionTypes_CustomSetCompanion.shared builder];
  // customSet = [customSetBuilder build];

  NSDictionary<id, id> *map = [J2ktiosinteropCollectionTypesCompanion.shared getMap];
  GKOTMutableDictionary<id, id> *hashMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getHashMap];
  GKOTMutableDictionary<id, id> *linkedHashMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getLinkedHashMap];
  GKOTMutableDictionary<id, id> *abstractMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getAbstractMap];
  GKOTMutableDictionary<id, id> *customMap =
      [J2ktiosinteropCollectionTypesCompanion.shared getCustomMap];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptIteratorWithJavaUtilIterator:iterator];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomIteratorWithJ2ktiosinteropCollectionTypes_CustomIterator:customIterator];

  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptListIteratorWithJavaUtilListIterator:listIterator];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomListIteratorWithJ2ktiosinteropCollectionTypes_CustomListIterator:
          customListIterator];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptIterableWithJavaLangIterable:iterable];
  [J2ktiosinteropCollectionTypesCompanion.shared
      acceptCustomIterableWithJ2ktiosinteropCollectionTypes_CustomIterable:customIterable];

  [J2ktiosinteropCollectionTypesCompanion.shared acceptCollectionWithJavaUtilCollection:collection];
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

  [J2ktiosinteropCollectionTypesCompanion.shared acceptSetWithJavaUtilSet:set];
  [J2ktiosinteropCollectionTypesCompanion.shared acceptHashSetWithJavaUtilHashSet:hashSet];
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
#else
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
#endif
}

- (void)testImmutableList {
#if J2KT
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
#else
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
#endif
}

- (void)testPrimitiveConstants {
  int i;
  i = JavaLangInteger_get_MAX_VALUE();
  i = JavaLangInteger_get_MIN_VALUE();
}

- (void)testThrowable {
  JavaLangThrowable *throwable;
  throwable = create_JavaLangThrowable_initWithNSString_(@"foo");
#if J2KT
  throwable = [[JavaLangThrowable alloc] initWithMessage:@"foo"];
#else
  throwable = [[JavaLangThrowable alloc] initWithNSString:@"foo"];
#endif
}

- (void)testInterface {
  id<J2ktiosinteropTestInterface> testInterface = [[TestImplementation alloc] init];
  [testInterface testMethod];

#if J2KT
  // TODO(b/284891929): Transpiled interfaces do not conform to NSObject protocol...
  // XCTAssertTrue([testInterface isKindOfClass:[TestImplementation class]]);

  // ...so explicit cast to id<NSObject> is required.
  XCTAssertTrue([((id<NSObject>)testInterface) isKindOfClass:[TestImplementation class]]);
#else
  XCTAssertTrue([testInterface isKindOfClass:[TestImplementation class]]);
#endif
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

#if J2KT
  // J2ktiosinteropCompileTimeConstants_CONSTANT_STRING is absent in Compat.h.
  // XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_CONSTANT_STRING, @"foo");
  // TODO(b/458647857): Should be false and not cause class initialization.
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());
#else
  XCTAssertEqualObjects(J2ktiosinteropCompileTimeConstants_CONSTANT_STRING, @"foo");
  XCTAssertFalse(J2ktiosinteropCompileTimeConstantInitialization_get_isInitialized());
#endif

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
#if J2KT
  // TODO(b/402759930): Not supported in J2KT.
  // XCTAssertEqual(J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethod), 1);
#else
  XCTAssertEqual(J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethod), 1);
#endif

  J2ktiosinteropInterfaceDefaultMethodOverrideImpl *interfaceDefaultMethodOverride =
      create_J2ktiosinteropInterfaceDefaultMethodOverrideImpl_init();
  XCTAssertEqual([interfaceDefaultMethodOverride getInt], 1);
  XCTAssertEqual([interfaceDefaultMethodOverride getDefaultLong], 2);
  XCTAssertEqual([interfaceDefaultMethodOverride getSuperDefaultLong], 1);
  // This function is designed to support `super` calls to default methods, but for the purpose of
  // this test can be used as a regular method.
#if J2KT
  // TODO(b/402759930): Not supported in J2KT.
  // XCTAssertEqual(
  //   J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethodOverride), 1);
#else
  XCTAssertEqual(
      J2ktiosinteropInterfaceDefaultMethod_getDefaultLong(interfaceDefaultMethodOverride), 1);
#endif
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
  XCTAssertEqual([propertyMethod getPropertyInOverrideMethod], 0);

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
  XCTAssertEqual([propertyMethodOverride getPropertyInOverrideMethod], 1);

  J2ktiosinteropPropertyAutoValueClass *propertyAutoValueClass =
      [[J2ktiosinteropPropertyAutoValueClass_builder() setNameWithNSString:@"foo"] build];
  XCTAssertEqual(propertyAutoValueClass.name, @"foo");

  J2ktiosinteropPropertyAutoValueMethod *propertyAutoValueMethod =
      [[J2ktiosinteropPropertyAutoValueMethod_builder() setNameWithNSString:@"foo"] build];
  XCTAssertEqual(propertyAutoValueMethod.name, @"foo");
}

- (void)testPlatform {
#if J2KT
  XCTAssertEqualObjects(J2ktiosinteropPlatform_get_NAME(), @"J2KT");
#else
  XCTAssertEqualObjects(J2ktiosinteropPlatform_get_NAME(), @"J2ObjC");
#endif
}

- (void)testNullability {
  J2ktiosinteropNullability_acceptNullableWithId_(nil);
  J2ktiosinteropNullability_acceptNullableWithNonNullBoundWithId_(nil);
  J2ktiosinteropNullability_acceptWithNullableBoundWithId_(nil);
  J2ktiosinteropNullability_acceptNullableWithNullableBoundWithId_(nil);
}

- (void)testObjectiveCNameOverrides {
  J2ktiosinteropObjectiveCNameOverrides_Parent *parent =
      create_J2ktiosinteropObjectiveCNameOverrides_Parent_init();
  XCTAssertEqualObjects([parent parent], @"parent");
  J2ktiosinteropObjectiveCNameOverrides_Child *child =
      create_J2ktiosinteropObjectiveCNameOverrides_Child_init();
  XCTAssertEqualObjects([child parent], @"parent/child");
  XCTAssertEqualObjects([child child], @"child");
}

- (void)testDataClassRecord {
  J2ktiosinteropDataClassRecord *record =
      create_J2ktiosinteropDataClassRecord_initWithInt_withNSString_(123, @"foo");
#if J2KT
  // TODO(b/445545563): Uncomment once Java records are translated to Kotlin data classes with
  // @JvmRecord annotation.
  // XCTAssertTrue([record isKindOfClass:[JavaLangRecord class]]);
#else
  XCTAssertTrue([record isKindOfClass:[JavaLangRecord class]]);
#endif
  XCTAssertEqual([record a], 123);
  XCTAssertEqual(record.a, 123);
  XCTAssertEqualObjects([record b], @"foo");
  XCTAssertEqualObjects(record.b, @"foo");
  XCTAssertTrue([record.description containsString:@"DataClassRecord"]);
  XCTAssertTrue([record.description containsString:@"123"]);
  XCTAssertTrue([record.description containsString:@"foo"]);

  J2ktiosinteropDataClassRecord *record2 =
      create_J2ktiosinteropDataClassRecord_initWithInt_withNSString_(123, @"foo");
  XCTAssertEqualObjects(record, record2);
  XCTAssertEqual(record.hash, record2.hash);
}

@end
