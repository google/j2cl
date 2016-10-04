#!/usr/grte/v4/bin/python2.7
"""Script for measuring cycle time for J2CL.

Takes a blaze target and a source file to modify to cause that target to
rebuild.  The target is rebuilt --iterations number of times in both a managed
clean citc client and the current client.
"""

import subprocess

import repo_util

from google3.pyglib import app
from google3.pyglib import flags

J2CL_WORKER_FLAGS = "--spawn_strategy=worker --internal_spawn_scheduler"

PROFILE_OUT_LOCATION = "/tmp/j2cl-blaze-profile.out"

WARM_UP_BUILDS = 30

FLAGS = flags.FLAGS

flags.DEFINE_string(
    "target", None,
    "The target to build. It should have a j2cl_transpile target in it's "
    "dependencies.")
flags.DEFINE_string("file", None,
                    "The file to modify while testing reload time.")
flags.DEFINE_integer("iterations", 40, "Number of times to rebuild the target.")


class BlazeTargetBenchmarker(object):
  """The benchmark class."""

  def __init__(self):
    self.current_working_dir = None

  def subprocess(self, command):
    subprocess.call(
        command,
        stdout=subprocess.PIPE,
        stderr=None,
        shell=True,
        cwd=self.current_working_dir)

  def blaze_build(self, blaze_target):
    self.subprocess("blaze build %s --profile=%s %s" % (J2CL_WORKER_FLAGS,
                                                        PROFILE_OUT_LOCATION,
                                                        blaze_target))

  def analyze_profile(self):
    result = subprocess.Popen(
        "blaze analyze-profile %s --dump=raw" % PROFILE_OUT_LOCATION,
        stdout=subprocess.PIPE,
        stderr=None,
        shell=True,
        cwd=self.current_working_dir)

    for line in result.stdout:  # read and store result in log file.
      components = line.split("|")
      (_, _, _, _, time_nano, _, action_type, action_message) = components

      if action_type == "SKYFUNCTION" and action_message.startswith(
          "ACTION_EXECUTION") and "_j2cl_transpile" in action_message:
        return int(time_nano) / 1000000.0

  def run(self, blaze_target, file_to_modify):
    """Runs the benchmark in both a clean client and the current client."""
    print "target: %s" % blaze_target
    print "file: %s" % file_to_modify

    synced_to_cl = repo_util.compute_synced_to_cl()
    repo_util.managed_repo_sync_to(synced_to_cl)

    # Execute benchmark in managed client
    print "Collecting original time in managed client."
    self.current_working_dir = repo_util.get_managed_path()
    original_times = self.perform_test(blaze_target, file_to_modify)

    # Execute benchmark on local client
    print "Collecting new time in current client."
    self.current_working_dir = None
    new_times = self.perform_test(blaze_target, file_to_modify)

    print "==== Summary Original Times ===="
    self.print_summary(original_times)

    print "==== Summary New Times ===="
    self.print_summary(new_times)

  def print_summary(self, times):
    print "J2CL fastest: %.2f ms" % (min(times))
    print "J2CL slowest: %.2f ms" % (max(times))
    print "J2CL mean: %.2f ms" % (sum(times) / len(times))
    print "J2CL median: %.2f ms" % (sorted(times)[len(times) / 2])

  def perform_test(self, blaze_target, file_to_modify):
    """Runs the benchmark."""
    times_ms = []
    if not repo_util.is_git():
      self.subprocess("g4 open %s" % file_to_modify)

    self.blaze_build(blaze_target)

    for _ in range(FLAGS.iterations):
      print "Modifying file and building again..."

      # Add white space to the front of the file and build again.
      self.subprocess('echo " " >> %s' % file_to_modify)

      self.blaze_build(blaze_target)
      time_ms = self.analyze_profile()
      print "J2CL time: %.2f ms" % (time_ms)
      times_ms.append(time_ms)

    return times_ms[WARM_UP_BUILDS:]


def main(unused_argv):
  assert FLAGS.target is not None
  assert FLAGS.file is not None
  assert WARM_UP_BUILDS < FLAGS.iterations
  BlazeTargetBenchmarker().run(FLAGS.target, FLAGS.file)


if __name__ == "__main__":
  app.run()
