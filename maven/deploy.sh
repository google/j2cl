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
#     <id>sonatype-nexus-staging</id>
#     <username>...</username>
#     <password>...</password>
#   </server>
# </servers>
#
readonly BAZEL_ROOT=$(pwd)

common::error() {
  echo "Error: $@" >&2
  exit 1
}

common::info() {
  echo "Info: $@"
}

#######################################
# Check if Bazel is installed and the script is run from the root of the Bazel
# repository.
# Globals:
#   BAZEL
#######################################
common::check_bazel() {
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
}

common::check_maven() {
  if ! command -v mvn &> /dev/null; then
    common::error "Maven is not installed. Please install it."
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

common::bazel_build() {
  local target="$1"
  common::info "Building Bazel target: ${target}"
  "${BAZEL}" build "${target}"
}


common::_parse_arguments() {
  merge_src=true
  deploy_to_sonatype=true
  artifact=""
  jar_file=""
  src_jar=""
  javadoc_jar=""
  license_header=""
  pom_template=""
  lib_version=""
  group_id=""
  artifact_directory=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --artifact)
        shift
        artifact="$1"
        ;;
      --jar-file)
        shift
        jar_file="$1"
        ;;
      --src-jar)
        shift
        src_jar="$1"
        ;;
      --javadoc-jar)
        shift
        javadoc_jar="$1"
        ;;
      --license-header)
        shift
        license_header="$1"
        ;;
      --pom-template)
        shift
        pom_template="$1"
        ;;
      --lib-version)
        shift
        lib_version="$1"
        ;;
      --group-id)
        shift
        group_id="$1"
        ;;
      --no-merge-src)
        merge_src=false
        ;;
      --no-deploy)
        deploy_to_sonatype=false
        ;;
      --artifact_dir)
        shift
        artifact_directory="$1"
        ;;
      *)
        echo "Error: Unknown flag '$1'"
        exit 1
        ;;
    esac
    shift
  done
}

common::_check_prerequisites() {
  # Check for required arguments
  if [[ -z "${artifact}" || -z "${jar_file}" || -z "${src_jar}" || -z "${javadoc_jar}" || \
        -z "${pom_template}" || -z "${lib_version}" || -z "${group_id}" ]]; then
    echo "Error: Missing required arguments."
    exit 1
  fi

  if [[ ${deploy_to_sonatype} == true ]] && [[ -z ${MAVEN_GPG_PASSPHRASE:-} ]]; then
    # If gpg_passphrase is unset, ask the user to provide it.
    echo "Enter your gpg passphrase:"
    read -s MAVEN_GPG_PASSPHRASE
  fi
}

common::_create_artifact() {
  jar cf "$1" .
  mv "$1" "$2"
}

common::_prepare_artifact_for_deployment() {
  classes_directory=$(mktemp -d)
  srcs_directory=$(mktemp -d)
  if [[ -z "${artifact_directory}" ]]; then
    artifact_directory=$(mktemp -d)
  fi

  cd "${srcs_directory}"

  # - extract src and class files in order to merge them in one jar if requested.
  # - sonatype requires a separate jar file containing only the sources
  # - Add license header on each src file.
  jar xf "${src_jar}"

  if [[ -n "${license_header}" ]]; then
    local tmp_file=$(mktemp)

    for java in $(find "${srcs_directory}" -name '*.java'); do
      cat "${license_header}" "${java}" > "${tmp_file}"
      mv "${tmp_file}" "${java}"
    done
  fi

  # source file for sonatype
  common::_create_artifact "${artifact}-sources.jar" "${artifact_directory}"

  if [[ "${merge_src}" == true ]]; then
    # Copy srcs with class files
    cp -r * "${classes_directory}"
  fi

  cd "${classes_directory}"
  jar xf "${jar_file}"

  # create the jar file that contains source and class files
  common::_create_artifact "${artifact}.jar" "${artifact_directory}"

  # Create javadoc jar file
  cp "${javadoc_jar}" "${artifact_directory}/${artifact}-javadoc.jar"
  # Replace version in template and generate the final pom.xml
  sed -e "s/__VERSION__/${lib_version}/g" -e "s/__ARTIFACT_ID__/${artifact}/g" -e "s/__GROUP_ID__/${group_id}/g" "${pom_template}" > "${artifact_directory}/pom.xml"

  cd "$BAZEL_ROOT" # Go back to the original directory
}

common::deploy_to_sonatype() {
  common::_parse_arguments "$@"
  common::_check_prerequisites
  common::_prepare_artifact_for_deployment


  if [[ "${deploy_to_sonatype}" == true ]]; then
    # Use maven to sign and deploy jar, sources jar and javadocs jar to OSS sonatype
    cd "${artifact_directory}"

    for i in "" sources javadoc; do
      local classifier_opts=""
      local suffix=""

      if [[ -n "$i" ]]; then
        classifier_opts="-Dclassifier=$i"
        suffix="-$i"
      fi

      mvn gpg:sign-and-deploy-file \
        -Dfile="${artifact}${suffix}.jar" \
        -DrepositoryId=sonatype-nexus-staging \
        -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2 \
        -Dpackaging=jar \
        -DartifactId="${artifact}" \
        -DgroupId="${group_id}" \
        -Dversion="${lib_version}" \
        -DpomFile=pom.xml "${classifier_opts}"
    done
    common::info "Deployment completed."
  else
    common::info "Artifacts created in ${artifact_directory}"
    common::info "Deployment not requested. No artifacts will be uploaded."
    # set artifact_directory to empty string so it's not deleted in cleanup_temp_files
    artifact_directory=""
  fi

  common::cleanup_temp_files
}

common::cleanup_temp_files() {
  if [[ -d "${artifact_directory:-}" ]]; then
    rm -rf "${artifact_directory}"
  fi
  if [[ -d "${classes_directory:-}" ]]; then
    rm -rf "${classes_directory}"
  fi
  if [[ -d "${srcs_directory:-}" ]]; then
    rm -rf "${srcs_directory}"
  fi
}

common::create_and_push_git_tag() {
  local lib_version="$1"
  common::info "Creating git tag: ${lib_version}"
  git tag -a "${lib_version}" -m "${lib_version} release"
  git push origin "${lib_version}"
}
