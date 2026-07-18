package org.plishka.backend.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessMetricsRecorder {
    private final MeterRegistry meterRegistry;

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCheckoutAttempt(String outcome) {
        counter("checkout.attempt", "outcome", outcome).increment();
    }

    public void recordCheckoutDuration(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("checkout.duration")
                .description("Checkout operation duration")
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void recordOrderCreated(long totalPrice, int itemsCount) {
        counter("order.created").increment();
        orderTotalPriceSummary().record(totalPrice);
        summary("order.items.count", "items").record(itemsCount);
    }

    public void recordCartOperation(String operation, String outcome) {
        counter("cart.operation", "operation", operation, "outcome", outcome).increment();
    }

    public void recordCallbackRequestCreated() {
        counter("callback.request.created").increment();
    }

    public void recordCallbackRequest(String outcome) {
        counter("callback.request", "outcome", outcome).increment();
    }

    public void recordAuthFlow(String operation, String outcome) {
        counter("auth.flow", "operation", operation, "outcome", outcome).increment();
    }

    public void recordProductViewRecord(String outcome) {
        counter("product.view.record", "outcome", outcome).increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }

    private DistributionSummary summary(String name, String baseUnit) {
        return DistributionSummary.builder(name)
                .baseUnit(baseUnit)
                .register(meterRegistry);
    }

    private DistributionSummary orderTotalPriceSummary() {
        return DistributionSummary.builder("order.total.price")
                .baseUnit("uah")
                .serviceLevelObjectives(1000, 3000, 5000, 10000, 20000, 50000, 100000)
                .register(meterRegistry);
    }
}
