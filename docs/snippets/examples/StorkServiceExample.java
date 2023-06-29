package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.loadbalancer.poweroftwochoices.OnePlusBetaConfiguration;
import io.smallrye.stork.loadbalancer.poweroftwochoices.OnePlusBetaLoadBalancer;
import io.smallrye.stork.loadbalancer.poweroftwochoices.PowerOfTwoChoicesConfiguration;
import io.smallrye.stork.loadbalancer.random.RandomConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import org.apache.commons.math3.analysis.function.Power;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StorkServiceExample {

    public static void runSingleExperiment(double beta) {
        Stork.initialize();
        Stork stork = Stork.getInstance();

        String host = "localhost";
        int startingPort = 8000;
        int numPorts = 5; // 20
        int numAllocators = 8; // 60
        int numAllocations = 2000; // 50
        int taskMillis = 5;


        StringBuilder destinations = new StringBuilder();
        for (int port = startingPort; port < startingPort + numPorts; ++port) {
            destinations.append(host).append(':').append(port).append(',');
        }

        stork.defineIfAbsent("my-service", ServiceDefinition.of(
                new StaticConfiguration().withAddressList(destinations.toString()),
                new OnePlusBetaConfiguration().withBeta(Double.toString(beta))
        ));

        // LoggerFactory.getLogger(StorkServiceExample.class).debug("Simple debug log here.");
        Service service = stork.getService("my-service");
        service.getServiceDiscovery().initialize(stork);

        ((OnePlusBetaLoadBalancer) service.getLoadBalancer())
                    .setInstances(service.getInstances().await().indefinitely());

        // Step 1. Create the allocators.
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numAllocators; ++i) {
            Allocator allocator = new Allocator(stork, numAllocations, taskMillis);
            threads.add(new Thread(allocator));
        }
        // Step 2. Run the allocators
        for (Thread thread : threads) {
            thread.start();
        }
        // Step 3. Join the threads.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
        }

        ((OnePlusBetaLoadBalancer) service.getLoadBalancer())
                .printCounters();

        // Step 4. Compute the total allocations for each port.
        /* HashMap<Integer, Integer> portAllocations = new HashMap<>();
        int maxAllocations = 0;
        int totalAllocations = 0;
        for (Allocator allocator : allocators) {
            for (Map.Entry<Integer, Integer> entry : allocator.portCount.entrySet()) {
                int key = entry.getKey();
                int value = entry.getValue();
                int newValue = value + portAllocations.getOrDefault(key, 0);

                portAllocations.put(key, newValue);
                totalAllocations += value;
                maxAllocations = Math.max(maxAllocations, newValue);
            }
        } */

        // Print the maximum
        // int normalisedMaxLoad = maxAllocations - (totalAllocations / numPorts);
        // System.out.println("Maximum load : " + normalisedMaxLoad);

        /* for (ServiceInstance instance : service.getServiceDiscovery().getServiceInstances()) {

        } */

        Stork.shutdown();
        // return normalisedMaxLoad;
    }

    /* public static double runMultipleExperiments(double beta, int reps) {
        int total = 0;
        for (int rep = 0; rep < reps; ++rep) {
            total += runSingleExperiment(beta);
        }
        return total / (double) reps;
    } */

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public static void runTwoChoice() {
        runSingleExperiment(1.0);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public static void runOnePlusBeta8() {
        runSingleExperiment(1.0);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public static void runOnePlusBeta9() {
        long start = System.currentTimeMillis();
        runSingleExperiment(0.9);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println(timeElapsed);
    }

    public static void main(String[] args) throws RunnerException {
        /* Options opt = new OptionsBuilder()
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .include(StorkServiceExample.class.getSimpleName()).build();

        new Runner(opt).run();
        System.out.println(Runtime.getRuntime().availableProcessors());
         */
        runOnePlusBeta8();
    }

    private static class Consumer implements Runnable {

        private final List<Uni<ServiceInstance>> instances;

        private final long taskMillis;

        double sampleExp() {
            return - taskMillis * Math.log(Math.random());
        }

        Consumer(List<Uni<ServiceInstance>> instances, long taskMillis) {
            this.instances = instances;
            this.taskMillis = taskMillis;
        }

        @Override
        public void run() {
            for (Uni<ServiceInstance> uniInstance : instances) {
                ServiceInstance instance = uniInstance.await().indefinitely();
                try {
                    Thread.sleep((long) sampleExp());
                } catch (InterruptedException ex) {
                    System.out.println(ex);
                }
                instance.recordEnd(null);
            }
        }
    }

    private static class Allocator implements Runnable {
        final Stork stork;
        final int numAllocations;
        final long taskMillis;

        Allocator(Stork stork, int numAllocations, long taskMillis) {
            this.stork = stork;
            this.numAllocations = numAllocations;
            this.taskMillis = taskMillis;
        }

        @Override
        public void run() {
            List<Uni<ServiceInstance>> uniInstances = new ArrayList<>();
            for (int i = 0; i < numAllocations; ++i) {
                // uniInstances.add();
                stork.getService("my-service")
                        .selectInstanceAndRecordStart(false).await().indefinitely();
            }
            // new Thread(new Consumer(uniInstances, taskMillis)).run();
        }

    }

}
