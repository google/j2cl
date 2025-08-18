"""Build J2KT interop headers."""

from collections.abc import Sequence
import os
import re
import zipfile

from absl import app


def _strip_blaze_out_prefix(path: str) -> str:
  """Strips the blaze-out prefixes from a path."""
  return re.sub(r'^blaze-out/[^/]*/(bin|genfiles)/?', '', path)


# Considerer rewriting this function to instead lookpup the path based on
# blaze plackage location (this can be complex based on globs, subdirections
# etc. and should only be considered if there is a performance benefit.)
def extract_package_from_stream(file_stream) -> str:
  """Extracts the package name from a Java file stream."""
  package_pattern = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)\s*;\s*$')
  for line in file_stream:
    line = line.decode('utf-8')
    match = package_pattern.match(line)
    if match:
      return match.group(1).replace('.', '/')
  return ''


def process_java_file(
    java_file: str,
    java_package: str,
    package_header_str: str,
    output_dir: str,
):
  """Generates Objective-C headers for a single Java file.

  Args:
    java_file: Path to the input Java file.
    java_package: The java package of the Java file written with slashes.
    package_header_str: String containing the package header.
    output_dir: Directory where the generated headers will be placed.
  """

  java_file_name = os.path.basename(java_file)
  java_dir_name = os.path.dirname(java_file)

  output_hdr_name = os.path.splitext(java_file_name)[0] + '.h'

  # Used for writing to the java_package header.
  full_path_loc = os.path.join(java_dir_name, output_hdr_name)

  # Used for actually writing the header.
  full_path = os.path.join(output_dir, java_dir_name)
  os.makedirs(full_path, exist_ok=True)
  with open(os.path.join(full_path, output_hdr_name), 'w') as output_hdr_file:
    output_hdr_file.write(package_header_str)

  full_java_package_path = os.path.join(
      output_dir,
      java_package,
  )
  if full_java_package_path != full_path:
    os.makedirs(full_java_package_path, exist_ok=True)
    with open(
        os.path.join(full_java_package_path, output_hdr_name), 'w'
    ) as output_hdr_file:
      output_hdr_file.write(f'#import "{full_path_loc}"\n')


def generate_package_header(framework_header: str, imports: list[str]) -> str:
  package_header = []
  # Write preamble to the temporary header file
  package_header.append('#pragma clang diagnostic push\n')
  package_header.append(
      '#pragma clang diagnostic ignored "-Wobjc-property-no-attribute"\n'
  )
  package_header.append('#import "{}"\n'.format(framework_header))
  package_header.append('#pragma clang diagnostic pop\n\n')
  for imp in imports:
    package_header.append('#import "{}"\n'.format(imp))
  return ''.join(package_header)


# TODO(b/254524002): Delete this path once j2kt_apple_framework is removed.
def process_jar(
    jar_path: str,
    imports: list[str],
    output_dir: str,
    framework_header: str,
) -> None:
  """Generates Objective-C headers for Java files within a JAR.

    Each Java source file in the source jar will get an identical header
    looking like:
    #pragma clang diagnostic push
    #pragma clang diagnostic ignored "-Wobjc-property-no-attribute"
    #import <package>/<target>.framework/Headers/<target>.h
    #pragma clang diagnostic pop
    #import <transpiler_generated_header1>
    #import <transpiler_generated_header2>

  Args:
    jar_path: Path to the input JAR file.
    imports: List of additional import paths.
    output_dir: Directory where the generated headers will be placed.
    framework_header: Path to the framework header.
  """
  package_header = generate_package_header(framework_header, imports)

  with zipfile.ZipFile(jar_path, 'r') as jar_zip:
    java_files = [name for name in jar_zip.namelist() if name.endswith('.java')]

    for java_file in java_files:
      java_file = _strip_blaze_out_prefix(java_file)

      with jar_zip.open(java_file) as file_stream:
        java_package = extract_package_from_stream(file_stream)
      process_java_file(java_file, java_package, package_header, output_dir)


def process_srclist(
    java_files: list[str],
    imports: list[str],
    output_dir: str,
    framework_header: str,
) -> None:
  """Generates Objective-C headers for Java files within a JAR."""

  package_header = generate_package_header(framework_header, imports)

  for java_file in java_files:
    with open(java_file, 'rb') as file_stream:
      java_package = extract_package_from_stream(file_stream)
    java_file = _strip_blaze_out_prefix(java_file)
    process_java_file(java_file, java_package, package_header, output_dir)


def main(argv: Sequence[str]) -> None:
  # TODO(b/254524002): When j2kt_apple_framework rule is removed, simplify
  # parsing by using named parameters with argparse insteand of manually parsing
  # them.
  arg_it = iter(argv)
  _ = next(arg_it)  # Skip the first argument, which is the binary name.
  output_dir = next(arg_it)
  framework_header = next(arg_it)

  os.makedirs(output_dir, exist_ok=True)

  for jar_path_or_srclist, imports_str in zip(*[iter(arg_it)] * 2):
    imports = [x for x in imports_str.split(':') if x]

    if jar_path_or_srclist.endswith('.jar') or jar_path_or_srclist.endswith(
        '.srcjar'
    ):
      process_jar(jar_path_or_srclist, imports, output_dir, framework_header)
    else:
      java_files = [x for x in jar_path_or_srclist.split(':') if x]
      process_srclist(java_files, imports, output_dir, framework_header)


if __name__ == '__main__':
  app.run(main)
