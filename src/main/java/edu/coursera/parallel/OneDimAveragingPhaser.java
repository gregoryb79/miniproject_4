package edu.coursera.parallel;

import java.util.concurrent.Phaser;

/**
 * Wrapper class for implementing one-dimensional iterative averaging using
 * phasers.
 */
public final class OneDimAveragingPhaser {
    /**
     * Default constructor.
     */
    private OneDimAveragingPhaser() {
    }

    /**
     * Sequential implementation of one-dimensional iterative averaging.
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *        iterative averaging problem
     * @param n The size of this problem
     */
    public static void runSequential(final int iterations, final double[] myNew,
            final double[] myVal, final int n) {
        double[] next = myNew;
        double[] curr = myVal;

        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                next[j] = (curr[j - 1] + curr[j + 1]) / 2.0;
            }
            double[] tmp = curr;            
            curr = next;
            next = tmp;
        }
    }

    /**
     * An example parallel implementation of one-dimensional iterative averaging
     * that uses phasers as a simple barrier (arriveAndAwaitAdvance).
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *        iterative averaging problem
     * @param n The size of this problem
     * @param tasks The number of threads/tasks to use to compute the solution
     */
    public static void runParallelBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {
        Phaser ph = new Phaser(0);
        ph.bulkRegister(tasks);
        // System.out.println("Running runParallelBarrier with " + tasks + " tasks");
        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                final int chunkSize = (n + tasks - 1) / tasks;
                final int left = (i * chunkSize) + 1;
                int right = (left + chunkSize) - 1;
                if (right > n) right = n;

                for (int iter = 0; iter < iterations; iter++) {
                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                            + threadPrivateMyVal[j + 1]) / 2.0;
                    }
                    ph.arriveAndAwaitAdvance();

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A parallel implementation of one-dimensional iterative averaging that
     * uses the Phaser.arrive and Phaser.awaitAdvance APIs to overlap
     * computation with barrier completion.
     *
     * TODO Complete this method based on the provided runSequential and
     * runParallelBarrier methods.
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *              iterative averaging problem
     * @param n The size of this problem
     * @param tasks The number of threads/tasks to use to compute the solution
     */
    public static void runParallelFuzzyBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {

        Phaser ph = new Phaser(tasks);
        Thread[] threads = new Thread[tasks];
        final int chunkSize = (n + tasks - 1) / tasks;
        myNew[1] = (myVal[0]+myVal[2])/2.0;
        myNew[n] = (myVal[n-1]+myVal[n+1])/2.0;

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;
            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int iter = 0; iter < iterations; iter++) {
                    final int left = (i * chunkSize) + 1;
                    int right = (left + chunkSize) - 1;
                    if (right > n) right = n;
                    
                    if (left <= right) {
                        threadPrivateMyNew[left] = (threadPrivateMyVal[left - 1] + threadPrivateMyVal[left + 1]) / 2.0;
                        if (right != left) {
                            threadPrivateMyNew[right] = (threadPrivateMyVal[right - 1] + threadPrivateMyVal[right + 1]) / 2.0;
                        }
                    }

                    int currPhase = ph.arrive();
                    
                    for (int j = left + 1; j < right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1] + threadPrivateMyVal[j + 1]) / 2.0;
                    }

                    ph.awaitAdvance(currPhase);

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final int n = 10_000_000;
        final int iterations = 1_000;
        final int tasks = 2;

        // double[] inputSeq = new double[n + 2];
        double[] inputPar = new double[n + 2];
        double[] inputFPar = new double[n + 2];
        // double[] outputSeq = new double[n + 2];
        double[] outputFPar = new double[n + 2];
        double[] outputPar = new double[n + 2];
        
        // inputSeq[n + 1] = 1.0;
        inputPar[n + 1] = 1.0;
        inputFPar[n + 1] = 1.0;
        // outputSeq[n + 1] = 1.0;
        outputPar[n + 1] = 1.0;
        outputFPar[n + 1] = 1.0;
        

        // Initialize input with some values
        // for (int i = 0; i < n + 2; i++) {
        //     // input[i] = i;
        //     output[i] = i;
        // }

        // System.out.println("Input:");
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", inputSeq[i]);
        // }
        
        System.out.println();
        System.out.println("runParallelBarrier:");
        double startTime = System.nanoTime();
        runParallelBarrier(iterations, outputPar, inputPar, n, tasks);
        double parTime = (System.nanoTime() - startTime);
        System.out.println("runParallelFuzzyBarrier:");
        startTime = System.nanoTime();
        runParallelFuzzyBarrier(iterations, outputFPar, inputFPar, n, tasks);
        double fParTime = (System.nanoTime() - startTime);
        // System.out.println("runSequential:");
        // startTime = System.nanoTime();
        // runSequential(iterations, outputSeq, inputSeq, n);
        // double seqTime = (System.nanoTime() - startTime);
        // System.out.println("Sequential time: " + seqTime / 1_000_000 + " ms");
        System.out.println("Parallel time: " + parTime / 1_000_000 + " ms");
        System.out.println("Fuzzy Parallel time: " + fParTime / 1_000_000 + " ms");
        // System.out.println("Speedup: " + (seqTime / parTime));
        // System.out.println("Fuzzy Speedup: " + (seqTime / fParTime));
        System.out.println("Parallel to Fuzzy speedup: " + (parTime / fParTime));
        

        // System.out.println("Seq output after " + iterations + " iterations:");
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", outputSeq[i]);
        // }
        // System.out.println();
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", inputSeq[i]);
        // }
        // System.out.println();
        // System.out.println("Par output after " + iterations + " iterations:");
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", outputPar[i]);
        // }
        // System.out.println();
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", inputPar[i]);
        // }
        // System.out.println();
        // System.out.println("FPar output after " + iterations + " iterations:");
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", outputFPar[i]);
        // }
        // System.out.println();
        // for (int i = 0; i < n + 2; i++) {
        //     System.out.printf("%.2f ", inputFPar[i]);
        // }
        // System.out.println();
    }
}
