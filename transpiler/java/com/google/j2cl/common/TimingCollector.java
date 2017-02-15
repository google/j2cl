package com.google.j2cl.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Stopwatch;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * Creates a report of the run time for J2CL.
 *
 * <p>The TimingCollector has 3 public methods: startSample startSubSample and endSubSample. This
 * allows for multiple levels of sampling. In a specific level you call startSample("sampleName")
 * which associates "sampleName" with the execution time up until the next time startSample or
 * endSubSample is called. You can record a more granular set of samples within a sample by calling
 * {@code startSubSample("sampleName")}. This sample and subsequent startSamples will be recorded as
 * sub-samples of the sample that was in effect when startSubSample is called. When a sub-sample is
 * finished you call endSubSample to yield the TimingCollector back up to the previous level.
 *
 * <p>Note that you may want to sample a block of code that gets executed many times. All the
 * samples of the same name will be aggregated for the report.
 *
 * <p>For example you may want to sample a method like this: <code><pre>
 * void run() {
 *   TimingCollector.get().startSample("run loop");
 *   int a = 1;
 *   for (int i = 0; i < 1000000; i++) {
 *     TimingCollector.get().startSubSample("add 1");
 *     a ++;
 *     TimingCollector.get().startSample("subtract 2");
 *     a -= 2;
 *     TimingCollector.get().endSubSample();
 *   }
 *   TimingCollector.get().startSample("sleep");
 *   Thread.sleep(1000);
 * }
 * </pre>
 *
 * This will result in a report: <pre>
 *   ...
 *   run loop X ms
 *     add 1 Y ms
 *     subtract 2 Z ms
 *   sleep 1000 ms
 *   ...
 * </pre>
 *
 * <p>Note that in this example the report will show that the add and subtract samples don't quite
 * add up to the time of the enclosing sample "run loop" since we don't account for the time taken
 * to run the statements in the for loop.
 *
 * <p></code>
 */
public class TimingCollector {
  private final Stack<Sample> sampleStack = new Stack<>();
  private static final ThreadLocal<TimingCollector> sharedInstance = new ThreadLocal<>();

  private static class Sample {
    final String name;
    private final Stopwatch stopwatch;
    private final List<Sample> subSamples = new ArrayList<>();

    Sample(String name) {
      this.stopwatch = Stopwatch.createStarted();
      this.name = name;
    }
  }

  public TimingCollector() {
    sampleStack.push(new Sample("Compiler"));
  }

  public static TimingCollector get() {
    if (sharedInstance.get() == null) {
      sharedInstance.set(new TimingCollector());
    }
    return sharedInstance.get();
  }

  private Sample getCurrentSample() {
    return sampleStack.peek();
  }

  /**
   * Begins sampling code following this execution. Note: The timer keeps running until the next
   * startSample(...) is called or endSubSample() is called, regardless of what path the code takes.
   * You must be careful when implementing timing code.
   */
  public void startSample(String sampleName) {
    Sample newSample = new Sample(sampleName);
    endCurrentSample();
    getCurrentSample().subSamples.add(newSample);
    sampleStack.push(newSample);
  }

  /** Ends the current sample. */
  private void endCurrentSample() {
    Sample sampleToEnd = getCurrentSample();
    checkArgument(sampleToEnd.stopwatch.isRunning());

    sampleToEnd.stopwatch.stop();
    sampleStack.pop();
  }

  /** Ends the current sample and yields timing up to the outer layer. */
  public void endSubSample() {
    endCurrentSample();
  }

  /**
   * Begin sub-sampling the current sample to get a break down of its time. Use endSubSample to
   * return to the previous sample level.
   */
  public void startSubSample(String firstSampleName) {
    checkState(!sampleStack.isEmpty());

    Sample newSample = new Sample(firstSampleName);
    getCurrentSample().subSamples.add(newSample);
    sampleStack.push(newSample);
  }

  private void printSamplesRecursive(List<Sample> samples, long parentTotalTimeMs, int depth) {
    String tab = "";
    for (int i = 0; i < depth; i++) {
      tab += "   ";
    }
    // Aggregate all sample by name.
    Multimap<String, Sample> samplesByName = LinkedHashMultimap.create();
    for (Sample sample : samples) {
      samplesByName.put(sample.name, sample);
    }
    // Print the sample data by name and recurse to its sub-samples.
    for (String name : samplesByName.keySet()) {
      long sumOfRunTimeMs = 0;
      List<Sample> subSamples = Lists.newArrayList();
      for (Sample sample : samplesByName.get(name)) {
        sumOfRunTimeMs += sample.stopwatch.elapsed(TimeUnit.MILLISECONDS);
        subSamples.addAll(sample.subSamples);
      }
      double ratio = sumOfRunTimeMs / (double) parentTotalTimeMs;
      System.out.println(tab + formatPercent(ratio) + " " + name + " " + sumOfRunTimeMs + " ms");
      printSamplesRecursive(subSamples, sumOfRunTimeMs, depth + 1);
    }
  }

  public void printReport() {
    Sample compilerSample = sampleStack.pop();
    compilerSample.stopwatch.stop();
    checkArgument(sampleStack.isEmpty(), "Sample was not finished ");

    System.out.println("Timing Report\n");
    printSamplesRecursive(
        Lists.newArrayList(compilerSample),
        compilerSample.stopwatch.elapsed(TimeUnit.MILLISECONDS),
        0);
    System.out.println();
  }

  private String formatPercent(double ratio) {
    return J2clUtils.format("%.2f", (ratio * 100.0)) + "%";
  }
}
