#!/bin/bash
# Copyright 2019 Google Inc. All Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS-IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# The script generates the open source artifacts needed for uploading a library to oss sonatype.
# The artifacts include the jar file, the sources jar file and the javadoc jar file.
# The script signs each jar and uploads them to sonatype.
#
# You need to install maven (3.0.5) before to run this script:
#  sudo apt-get install maven
#
# For using the gpg maven plugin, you need to create a gpg2 key.
# 1)  install gpg2 (1.4.16) if needed:
#   sudo apt-get install gnupg2
# 2) create a key pair by running:
#   gpg2 --gen-key
#
# The script will ask you the passphrase of your key if the MAVEN_GPG_PASSPHRASE
# environment variable is not set.
#
# You have to also configure the repository credentials by creating a settings.xml
# file in the ~/.m2/ directory and add this section :
# <servers>
#   <server>
#      <id>central</id>
#      <username>central_portal_token_username</username>
#      <password>central_portal_token_password</password>
#   </server>
# </servers>
#
# You can find the repository credentials in go/valentine
#
readonly BAZEL_ROOT=$(pwd)
readonly BUILD_SECTION_FILE="${BAZEL_ROOT}/maven/build_section.xml"

common::error() {
  echo "Error: $@" >&2
  exit 1
}

common::info() {
  echo "Info: $@"
}

#######################################
# Check if :
# - Bazel is installed,
# - the script is run from the root of the Bazel repository.
# - Bazel environment variables are set.
# Globals:
#   BAZEL
#   BAZEL_ARTIFACT
#   BAZEL_PATH
#######################################
common::check_bazel_prerequisites() {
  if [ ! -f "MODULE.bazel" ]; then
    common::error "This script must be run from the root of the Bazel repository (where MODULE.bazel exists)."
  fi

  # Check for Bazel or Bazelisk
  if command -v bazelisk &> /dev/null; then
    BAZEL="bazelisk"
  elif command -v bazel &> /dev/null; then
    BAZEL="bazel"
  else
    common::error "Neither Bazel nor Bazelisk is installed or in your PATH."
  fi

  if [[ -z "${BAZEL_ARTIFACT:-}" ]]; then
    common::error "BAZEL_ARTIFACT is not set."
  fi

  if [[ -z "${BAZEL_PATH:-}" ]]; then
    common::error "BAZEL_PATH is not set."
  fi
}

#######################################
# Check if :
# - Maven is installed.
# - MAVEN_ARTIFACT is set.
# - a pom xml file is present in the maven directory
# - global variables used for deployement are set.
# - check if MAVEN_GPG_PASSPHRASE env var is set. If not ask the user to provide
#.  it and store the passphrase in MAVEN_GPG_PASSPHRASE.
# Globals:
#   MAVEN_ARTIFACT
#   BAZEL_ROOT
#   MAVEN_GPG_PASSPHRASE
#######################################
common::check_maven_prerequisites() {
  if ! command -v mvn &> /dev/null; then
    common::error "Maven is not installed. Please install it."
  fi

  if [[ -z "${MAVEN_ARTIFACT:-}" ]]; then
    common::error "MAVEN_ARTIFACT is not set."
  fi

  # TODO(dramaix): Rename the pom files by using the maven artifact name.
  if [ ! -f "${BAZEL_ROOT}/maven/pom-${BAZEL_ARTIFACT}.xml" ]; then
    common::error "Maven pom file not found for artifact ${MAVEN_ARTIFACT}."
  fi

  # Every project that deploy to sonatype with maven needs to set the
  # deploy_to_sonatype variable to be able to skip the deployment step.
  if [[ -z "${deploy_to_sonatype:-}" ]]; then
    common::error "deploy_to_sonatype variable is missing."
  fi

  # sonatype_auto_publish is used to automatically publish the artifacts after
  # deployment.
  if [[ -z "${sonatype_auto_publish:-}" ]]; then
    common::error "sonatype_auto_publish variable is missing."
  fi

  if [[ -z ${MAVEN_GPG_PASSPHRASE:-} ]]; then
    # If gpg_passphrase is unset, ask the user to provide it.
    echo "Enter your gpg passphrase:"
    read -s MAVEN_GPG_PASSPHRASE
  fi
}

#######################################
# Check if --version flag is set and the value is valid.
# Every project requires a version number for deployment.
#######################################
common::check_version_set() {
  if [[ -z "$lib_version" ]]; then
    common::error "--version flag is missing"
  fi
  if [[ "$lib_version" == "--"* ]]; then
      common::error "Incorrect version value: $lib_version"
  fi
}

common::_bazel_build() {
  local target="$1"
  common::info "Building Bazel target: ${target}"
  "${BAZEL}" build "${target}"
}

common::build() {
  common::_bazel_build "//${BAZEL_PATH}:lib${BAZEL_ARTIFACT}.jar"
  common::_bazel_build "//${BAZEL_PATH}:lib${BAZEL_ARTIFACT}-src.jar"
  common::_bazel_build "//${BAZEL_PATH}:${BAZEL_ARTIFACT}-javadoc.jar"
}

common::_prepare_sources_artifact() {
  local maven_src_jar="$1"
  local bazel_src_jar="${BAZEL_ROOT}/bazel-bin/${BAZEL_PATH}/lib${BAZEL_ARTIFACT}-src.jar"

  if [[ -n "${LICENSE_HEADER_FILE:-}" ]]; then
    common::info "Adding license header to java source files."

    local srcs_directory=$(mktemp -d)
    jar xf "${bazel_src_jar}" -C "${srcs_directory}"

    local tmp_file=$(mktemp)

    for java in $(find "${srcs_directory}" -name '*.java'); do
      cat "${LICENSE_HEADER_FILE}" "${java}" > "${tmp_file}"
      mv "${tmp_file}" "${java}"
    done

    jar cf "${maven_src_jar}" -C "${srcs_directory}" .
    rm -rf "${srcs_directory}"
  else
    common::info "Copy sources jar to maven directory."
    cp "${bazel_src_jar}" "${maven_src_jar}"
  fi
}

common::_prepare_javadoc_artifact() {
  common::info "Copying javadoc jar to maven directory."
  local javadoc_jar="${BAZEL_ROOT}/bazel-bin/${BAZEL_PATH}/${BAZEL_ARTIFACT}-javadoc.jar"
  local maven_javadoc_jar="${maven_wd}/${MAVEN_ARTIFACT}-javadoc.jar"
  cp "${javadoc_jar}" "${maven_javadoc_jar}"
}

common::_extract_classes(){
  common::info "Extracting class files to maven directory."
  local maven_src_jar="$1"

  local classes_directory="${maven_wd}/${MAVEN_ARTIFACT}-classes"
  mkdir -p "${classes_directory}"

  local bazel_jar_file="${BAZEL_ROOT}/bazel-bin/${BAZEL_PATH}/lib${BAZEL_ARTIFACT}.jar"

  jar xf "${bazel_jar_file}" -C "${classes_directory}"

  if [[ "${COPY_SRCS_IN_JAR:-true}" == true ]]; then
    common::info "Extracting sources along with the class files."
    jar xf "${maven_src_jar}" -C "${classes_directory}"
  fi
}

common::_prepare_pom_file() {
  common::info "Generating pom file for ${MAVEN_ARTIFACT}."
  # Replace placeholders and generate the final pom.xml
  sed -e "s/__VERSION__/${lib_version}/g" \
    -e "/__BUILD_SECTION__/r ${BUILD_SECTION_FILE}" \
    -e "/__BUILD_SECTION__/d" \
    "${BAZEL_ROOT}/maven/pom-${BAZEL_ARTIFACT}.xml" > "${maven_wd}/pom-${MAVEN_ARTIFACT}.xml"
}

common::_prepare_artifact_for_sonatype() {
  local maven_src_jar="${maven_wd}/${MAVEN_ARTIFACT}-sources.jar"

  common::_prepare_sources_artifact "${maven_src_jar}"
  common::_prepare_javadoc_artifact
  # Extract the class files, maven will repackage everything in its own jar.
  common::_extract_classes "${maven_src_jar}"
  common::_prepare_pom_file
}

common::deploy_to_sonatype() {
  # TODO(Dramaix): Let the caller pass in the maven directory and be responsible
  # for setting the trap to cleanup the tmp directory
  if [[ -n "${1:-}" ]]; then
    common::info "Using provided directory '$1' for maven artifacts."
    maven_wd="$1"
  else
    common::info "Created temporary directory for maven artifacts."
    maven_wd=$(mktemp -d)
  fi

  common::_prepare_artifact_for_sonatype

  local pom_file="${maven_wd}/pom-${MAVEN_ARTIFACT}.xml"

  if [[ "${deploy_to_sonatype}" == true ]]; then
    common::info "Deploying artifacts to sonatype..."
    # Use maven to sign and deploy jar, sources jar and javadocs jar to OSS sonatype
    mvn -f "${pom_file}" clean deploy "-DautoPublish=${sonatype_auto_publish}"

    common::info "Deployment completed."
  else
    common::info "Packaging and installing maven artifacts locally..."
    mvn -f "${pom_file}" clean install

    common::info "Maven artifacts created in ${maven_wd}"
    common::info "Deployment not requested. No artifacts will be deployed."
    # set maven_wd to empty string so it's not deleted in cleanup_temp_files
    maven_wd=""
  fi

  common::cleanup_temp_files
}

common::cleanup_temp_files() {
  if [[ -d "${maven_wd:-}" ]]; then
    rm -rf "${maven_wd}"
  fi
}

common::create_and_push_git_tag() {
  local lib_version="$1"
  common::info "Creating git tag: ${lib_version}"
  git tag -a "${lib_version}" -m "${lib_version} release"
  git push origin "${lib_version}"
}
