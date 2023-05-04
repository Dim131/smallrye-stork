package io.smallrye.stork.loadbalancer.poweroftwochoices;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;
import io.smallrye.stork.loadbalancer.requests.InflightRequestCollector;

/**
 * Select two random destinations and then select the one with the least assigned requests. This avoids the overhead of
 * least-requests and the worst case for random where it selects a busy destination.
 */
public class PowerOfTwoChoicesLoadBalancer implements LoadBalancer {

    private final InflightRequestCollector collector = new InflightRequestCollector();
    private final Random random;

    /**
     * Creates a new instance of PowerOfTwoChoicesLoadBalancer.
     *
     * @param useSecureRandom {@code true} to use a {@link SecureRandom} instead of a regular {@link Random}
     */
    protected PowerOfTwoChoicesLoadBalancer(boolean useSecureRandom) {
        random = useSecureRandom ? new SecureRandom() : new Random();
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }

        List<ServiceInstance> instances = new ArrayList<>(serviceInstances);
        int count = instances.size();

        if (count == 1) {
            return instances.get(0);
        }

        ServiceInstance first = instances.get(random.nextInt(count));
        
        double beta = 0.5;
        if (random.nextDouble() > beta) {
           return new ServiceInstanceWithStatGathering(first, collector);
        }
        ServiceInstance second = instances.get(random.nextInt(count));

        int concurrencyOfFirst = collector.get(first.getId());
        int concurrencyOfSecond = collector.get(second.getId());

        if (concurrencyOfFirst < concurrencyOfSecond) {
            return new ServiceInstanceWithStatGathering(first, collector);
        } else {
            return new ServiceInstanceWithStatGathering(second, collector);
        }
    }

    @Override
    public boolean requiresStrictRecording() {
        return false;
    }
}
