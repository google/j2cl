#!/bin/bash -i
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
# The script will ask you the passphrase of your key.
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
set -e

merge_src=true
deploy_to_sonatype=true

while [[ "$1" != "" ]]; do
  case $1 in
    --artifact )        shift
                        artifact=$1
                        ;;
    --jar-file )        shift
                        jar_file=$1
                        ;;
    --src-jar )         shift
                        src_jar=$1
                        ;;
    --javadoc-jar)      shift
                        javadoc_jar=$1
                        ;;
    --license-header )  shift
                        license_header=$1
                        ;;
    --pom-template )    shift
                        pom_template=$1
                        ;;
    --lib-version )     shift
                        lib_version=$1
                        ;;
    --group-id )        shift
                        group_id=$1
                        ;;
    --gpg-passphrase )  shift
                        gpg_passphrase=$1
                        ;;
    --no-merge-src )    merge_src=false
                        ;;
    --no-deploy )       deploy_to_sonatype=false
                        ;;
    --artifact_dir )    shift
                        artifact_directory=$1
                        ;;
     * )                echo "Error: Unknown flag $1"
                        exit 1
                        ;;
  esac
  shift
done


if [[ ${deploy_to_sonatype} == true ]] && [[ -z ${gpg_passphrase} ]]; then
  # As this script can be called in a for loop for releasing several artifacts at once, the caller
  # script can pass the gpg_passphrase as argument to avoid to ask several time the user for the
  # passphrase.
  # If gpg_passphrase is unset, ask the user to provide it.
  echo "enter your gpg passphrase:"
  read -s gpg_passphrase
fi

create_artifact() {
  jar cf $1 .
  mv $1 $2
}

classes_directory=$(mktemp -d)
srcs_directory=$(mktemp -d)
if [[ -z ${artifact_directory} ]]; then
  artifact_directory=$(mktemp -d)
fi

cd ${srcs_directory}

# - extract src and class files in order to merge them in one jar if requested.
# - sonatype requires a separate jar file containing only the sources
# - Add license header on each src file.
jar xf ${src_jar}

if [[ -n "${license_header}" ]]; then
  tmp_file=$(mktemp)

  for java in $(find ${srcs_directory} -name '*.java'); do
    cat  ${license_header} ${java} > ${tmp_file}
    mv ${tmp_file} ${java}
  done
fi

# source file for sonatype
create_artifact "${artifact}-sources.jar" ${artifact_directory}

if [[ ${merge_src} == true ]]; then
  # Copy srcs with class files
  cp -r * ${classes_directory}
fi

cd ${classes_directory}
jar xf ${jar_file}

# create the jar file that contains source and class files
create_artifact "${artifact}.jar" ${artifact_directory}

# Create javadoc jar file
cp ${javadoc_jar} "${artifact_directory}/${artifact}-javadoc.jar"
# Replace version in template and generate the final pom.xml
sed -e "s/__VERSION__/${lib_version}/g" -e "s/__ARTIFACT_ID__/${artifact}/g" -e "s/__GROUP_ID__/${group_id}/g"  ${pom_template} > ${artifact_directory}/pom.xml

if [[ ${deploy_to_sonatype} == true ]]; then
  # Use maven to sign and deploy jar, sources jar and javadocs jar to OSS sonatype
  cd ${artifact_directory}

  for i in "" sources javadoc; do
    if [[ -n "$i" ]]; then
      classifier="-Dclassifier=$i"
      suffix="-$i"
    else
      classifier=""
      suffix=""
    fi
    mvn gpg:sign-and-deploy-file \
      -Dgpg.passphrase=${gpg_passphrase} \
      -Dfile=${artifact}${suffix}.jar \
      -DrepositoryId=sonatype-nexus-staging \
      -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2 \
      -Dpackaging=jar \
      -DartifactId=${artifact} \
      -DgroupId=${group_id} \
      -Dversion=${lib_version} \
      -DpomFile=pom.xml $classifier
  done

  rm -rf ${artifact_directory}
else
  echo "Artifacts created in ${artifact_directory}"
fi

rm -rf ${classes_directory}
rm -rf ${srcs_directory}

