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
# The artifacts includes the jar file, the sources jar file and the javadoc jar file.
# The script signs each jar and upload them to sonatype.
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
# You have to also configure the repository credentials by create a settings.xml
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

artifact=$1
jar_file=$2
src_jar=$3
license_header=$4
pom_template=$5
lib_version=$6
group_id=$7
gpg_passphrase=$8

if [ -z ${gpg_passphrase} ]; then
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

tmp_directory=$(mktemp -d)
artifact_directory=$(mktemp -d)

cd ${tmp_directory}

# - extract src and class files in order to merge them in one jar.
# - sonatype requires a separate jar file containing only the sources
# - Add license header on each src file.
jar xf ${src_jar}

if [ "${license_header}" -ne "--no-license" ]; then
  tmp_file=$(mktemp)

  for java in $(find ${tmp_directory} -name '*.java'); do
    cat  ${license_header} ${java} > ${tmp_file}
    mv ${tmp_file} ${java}
  done
fi

# source file for sonatype
create_artifact "${artifact}-sources.jar" ${artifact_directory}

jar xf ${jar_file}

# create the jar file that contains source and class files
create_artifact "${artifact}.jar" ${artifact_directory}

# Create javadoc jar file
javadoc_dest=$(mktemp -d)
find ${tmp_directory} -type f -name "*.java" | xargs javadoc -d ${javadoc_dest}

cd ${javadoc_dest}
create_artifact "${artifact}-javadoc.jar" ${artifact_directory}

# Replace version in template and generate the final pom.xml
sed -e "s/__VERSION__/${lib_version}/g" -e "s/__ARTIFACT_ID__/${artifact}/g" -e "s/__GROUP_ID__/${group_id}/g"  ${pom_template} > ${artifact_directory}/pom.xml

# Use maven to sign and deploy jar, sources jar and javadocs jar to OSS sonatype
cd ${artifact_directory}

for i in "" sources javadoc; do
  if [ -n "$i" ]; then
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

rm -rf ${javadoc_dest}
rm -rf ${tmp_directory}
rm -rf ${artifact_directory}
