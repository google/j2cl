import io
from unittest import mock

from google3.testing.pybase import googletest
from google3.testing.pybase import parameterized
from google3.third_party.java_src.j2cl.tools.j2kt import generate_j2kt_interop_header


def process_java_file_params(
    test_name: str,
    java_file: str,
    java_package: str,
    output_dir: str,
    expected_files: dict[str, str],
):
  return (
      test_name,
      java_file,
      java_package,
      output_dir,
      expected_files,
  )


class GenerateJ2ktInteropHeaderTest(parameterized.TestCase):

  @parameterized.named_parameters(
      process_java_file_params(
          test_name="basic_test",
          java_file="com/example/MyClass.java",
          java_package="com/example",
          output_dir="output",
          expected_files={
              "output/com/example/MyClass.h": "Package Header Src\n",
          },
      ),
      process_java_file_params(
          test_name="blaze_out_in_jar_file_path",
          java_file="java/com/google/communication/meetcore/xplat/plugins/batchjoin/BatchJoin.java",
          java_package=(
              "com/google/communication/meetcore/xplat/plugins/batchjoin"
          ),
          output_dir="my_output_dir",
          expected_files={
              "my_output_dir/java/com/google/communication/meetcore/xplat/plugins/batchjoin/BatchJoin.h": (
                  "Package Header Src\n"
              ),
              "my_output_dir/com/google/communication/meetcore/xplat/plugins/batchjoin/BatchJoin.h": (
                  "#import"
                  ' "java/com/google/communication/meetcore/xplat/plugins/batchjoin/BatchJoin.h"\n'
              ),
          },
      ),
      process_java_file_params(
          test_name="generated_java_class_in_higher_package",
          java_file="com/google/communication/meetcore/xplat/model/collections/GeneratedCollectionsModule.java",
          java_package=(
              "com/google/communication/meetcore/xplat/model/collections"
          ),
          output_dir="output",
          expected_files={
              "output/com/google/communication/meetcore/xplat/model/collections/GeneratedCollectionsModule.h": (
                  "Package Header Src\n"
              ),
          },
      ),
      process_java_file_params(
          test_name="super_sourced_in_jar",
          java_file="java/com/google/apps/xplat/util/concurrent/super-j2kt-native/com/google/apps/xplat/util/concurrent/IosExecutorFactory.java",
          java_package="com/google/apps/xplat/util/concurrent",
          output_dir="out_dir",
          expected_files={
              "out_dir/java/com/google/apps/xplat/util/concurrent/super-j2kt-native/com/google/apps/xplat/util/concurrent/IosExecutorFactory.h": (
                  "Package Header Src\n"
              ),
              "out_dir/com/google/apps/xplat/util/concurrent/IosExecutorFactory.h": (
                  "#import"
                  ' "java/com/google/apps/xplat/util/concurrent/super-j2kt-native/com/google/apps/xplat/util/concurrent/IosExecutorFactory.h"\n'
              ),
          },
      ),
      process_java_file_params(
          test_name="java_file_with_multiple_java_layers",
          java_file="transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.java",
          java_package="j2ktiosinterop",
          output_dir="out_dir",
          expected_files={
              "out_dir/transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h": (
                  "Package Header Src\n"
              ),
              "out_dir/j2ktiosinterop/CustomNames.h": (
                  "#import"
                  ' "transpiler/javatests/com/google/j2cl/integration/java/j2ktiosinterop/CustomNames.h"\n'
              ),
          },
      ),
  )
  @mock.patch(
      "google3.third_party.java_src.j2cl.tools.j2kt.generate_j2kt_interop_header.os.path.exists"
  )
  @mock.patch(
      "google3.third_party.java_src.j2cl.tools.j2kt.generate_j2kt_interop_header.os.makedirs"
  )
  @mock.patch("builtins.open", new_callable=mock.mock_open)
  def test_process_java_file(
      self,
      java_file: str,
      java_package: str,
      output_dir: str,
      expected_files: dict[str, str],
      mock_open: mock.Mock,
      mock_makedirs: mock.Mock,
      mock_exists: mock.Mock,
  ):
    del mock_makedirs
    mock_exists.return_value = False
    generate_j2kt_interop_header.process_java_file(
        java_file, java_package, "Package Header Src\n", output_dir
    )
    if expected_files:
      mock_open.assert_called()
      for key in expected_files.keys():
        try:
          mock_open.assert_any_call(key, "w")
        except AssertionError:
          print(f"Header not written for: {key}")
          print(f"Written headers {mock_open.mock_calls}")
          raise
        try:
          mock_open.return_value.write.assert_any_call(expected_files[key])
        except AssertionError:
          print(
              f"Assertion failed for file: {key} with expected:"
              f" {expected_files[key]}"
          )
          print("Calls to mock_open.return_value.write:")
          print(mock_open.return_value.write.mock_calls)
          raise
    else:
      try:
        mock_open.assert_not_called()
      except AssertionError:
        print(f"Expect no output files but wrote: {mock_open.mock_calls}")
        print("Calls to mock_open.return_value.write:")
        print(mock_open.return_value.write.mock_calls)
        raise

  @parameterized.named_parameters(
      ("normal_package", b"package com.google.example;", "com/google/example"),
      (
          "package_with_whitespace",
          b"  package   com.google.example  ;  ",
          "com/google/example",
      ),
      ("no_package", b"public class Test {}", ""),
      (
          "package_with_underscores",
          b"package com.google.example_test;",
          "com/google/example_test",
      ),
      ("empty_stream", b"", ""),
      (
          "multiple_lines",
          b"// Comment\npackage com.google.example;",
          "com/google/example",
      ),
  )
  def test_extract_package_from_stream(self, file_content, expected_package):
    file_stream = io.BytesIO(file_content)
    self.assertEqual(
        generate_j2kt_interop_header.extract_package_from_stream(file_stream),
        expected_package,
    )


if __name__ == "__main__":
  googletest.main()
