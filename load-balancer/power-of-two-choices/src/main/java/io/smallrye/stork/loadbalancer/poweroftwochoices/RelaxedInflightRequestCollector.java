package io.smallrye.stork.loadbalancer.requests;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.spi.CallStatisticsCollector;

/**
 * {@link CallStatisticsCollector} that keep tracks of the number of inflight requests.
 */
public class RelaxedInflightRequestCollector implements CallStatisticsCollector {

    private Map<Long, AtomicInteger> storage;

    private static class RelaxedCounter {
        volatile int cnt;

        void incrementAndGet() {
            ++cnt;
        }

        void decrementAndGet() {
            cnt = Math.max(0, cnt - 1);
        }

        int get() {
            return cnt;
        }

    }

    public void setInstances(Collection<ServiceInstance> serviceInstances) {
        HashMap<Long, AtomicInteger> storageCounters = new HashMap<>();
        for (ServiceInstance instance : serviceInstances) {
            storageCounters.put(instance.getId(), new AtomicInteger());
        }
        storage = Collections.unmodifiableMap(storageCounters);
    }

    /**
     * Gets the number of inflight requests for the service instance with the given {@code id}.
     *
     * @param id the service instance id
     * @return the number of inflight request, {@code 0} if none.
     */
    public int get(long id) {
        return storage.get(id).get();
    }

    @Override
    public void recordStart(long serviceInstanceId, boolean measureTime) {
        storage.get(serviceInstanceId).incrementAndGet();
    }

    @Override
    public void recordEnd(long serviceInstanceId, Throwable throwable) {
        storage.get(serviceInstanceId).decrementAndGet();
    }

    public void printCounters() {
        int mx = 0;
        for (Map.Entry<Long, AtomicInteger> entry : storage.entrySet()) {
            mx = Math.max(entry.getValue().get(), mx);
        }
        System.out.println("Max : " + mx);
    }
}
