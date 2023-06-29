package io.smallrye.stork.loadbalancer.poweroftwochoices;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * A load balancer provider following the <em>Power of two random choices</em> strategy.
 */
@LoadBalancerType("one-plus-beta")
@LoadBalancerAttribute(name = "use-secure-random", defaultValue = "false", description = "Whether the load balancer should use a SecureRandom instead of a Random (default). Check [this page](https://stackoverflow.com/questions/11051205/difference-between-java-util-random-and-java-security-securerandom) to understand the difference")
@LoadBalancerAttribute(name = "beta", defaultValue = "1.0", description = "The probability by which the load balancer samples two services instead of one.")
@ApplicationScoped
public class OnePlusBetaLoadBalancerProvider
        implements LoadBalancerProvider<OnePlusBetaConfiguration> {

    public LoadBalancer createLoadBalancer(OnePlusBetaConfiguration config,
                                           ServiceDiscovery serviceDiscovery) {
        return new OnePlusBetaLoadBalancer(
                Boolean.parseBoolean(config.getUseSecureRandom()),
                Double.parseDouble(config.getBeta()));
    }
}
