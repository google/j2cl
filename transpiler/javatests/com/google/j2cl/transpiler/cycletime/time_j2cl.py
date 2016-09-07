#!/usr/grte/v4/bin/python2.7
"""Script for measuring cycle time for J2CL.

Takes a blaze target and a source file to modify to cause that target to
rebuild.
"""

import subprocess

from google3.pyglib import app
from google3.pyglib import flags

J2CL_WORKER_FLAGS = "--spawn_strategy=worker --internal_spawn_scheduler"

PROFILE_OUT_LOCATION = "/tmp/j2cl-blaze-profile.out"

WARM_UP_BUILDS = 5

FLAGS = flags.FLAGS

flags.DEFINE_string(
    "target", None,
    "The target to build. It should have a j2cl_transpile target in it's "
    "dependencies.")
flags.DEFINE_string("file", None,
                    "The file to modify while testing reload time.")
flags.DEFINE_integer("iterations", 20, "Number of times to rebuild the target.")


class BlazeTargetBenchmarker(object):
  """The benchmark class."""

  def __init__(self):
    self.times_ms = []

  def blaze_build(self, blaze_target):
    subprocess.call(
        "blaze build %s --profile=%s %s" % (J2CL_WORKER_FLAGS,
                                            PROFILE_OUT_LOCATION, blaze_target),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        shell=True)

  def analyze_profile(self):
    result = subprocess.Popen(
        "blaze analyze-profile %s --dump=raw" % PROFILE_OUT_LOCATION,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        shell=True)

    for line in result.stdout:  # read and store result in log file.
      components = line.split("|")
      (_, _, _, _, time_nano, _, action_type, action_message) = components

      if action_type == "SKYFUNCTION" and action_message.startswith(
          "ACTION_EXECUTION") and "_j2cl_transpile" in action_message:
        time_ms = int(time_nano) / 1000000.0
        self.times_ms.append(time_ms)
        print "J2CL time: %.2f ms" % (time_ms)

  def run(self, blaze_target, file_to_modify):
    """Runs the benchmark."""
    print "target: %s" % blaze_target
    print "file: %s" % file_to_modify

    contents_original = open(file_to_modify, "r").read()

    self.blaze_build(blaze_target)

    for _ in range(FLAGS.iterations):
      print "Modifying file and building again..."

      # Add white space to the front of the file and build again.
      subprocess.call('echo " " >> %s' % file_to_modify, shell=True)

      self.blaze_build(blaze_target)
      self.analyze_profile()

    times = self.times_ms[WARM_UP_BUILDS:]

    print ""
    print "==== Summary ===="
    print "J2CL fastest: %.2f ms" % (min(times))
    print "J2CL slowest: %.2f ms" % (max(times))
    print "J2CL mean: %.2f ms" % (sum(times) / len(times))
    print "J2CL median: %.2f ms" % (sorted(times)[len(times) / 2])

    # Write back the original contents so that there are no changes.
    open(file_to_modify, "w").write(contents_original)


def main(unused_argv):
  assert FLAGS.target is not None
  assert FLAGS.file is not None
  BlazeTargetBenchmarker().run(FLAGS.target, FLAGS.file)


if __name__ == "__main__":
  app.run()
