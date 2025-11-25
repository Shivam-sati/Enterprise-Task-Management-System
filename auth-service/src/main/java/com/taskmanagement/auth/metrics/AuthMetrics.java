package com.taskmanagement.auth.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter loginAttempts;
    private final Counter loginSuccesses;
    private final Counter loginFailures;
    private final Counter registrations;
    private final Counter tokenRefreshes;
    private final Timer loginDuration;

    public AuthMetrics(MeterRegistry meterRegistry) {
        this.loginAttempts = Counter.builder("auth.login.attempts")
                .description("Total number of login attempts")
                .register(meterRegistry);
        
        this.loginSuccesses = Counter.builder("auth.login.successes")
                .description("Total number of successful logins")
                .register(meterRegistry);
        
        this.loginFailures = Counter.builder("auth.login.failures")
                .description("Total number of failed logins")
                .register(meterRegistry);
        
        this.registrations = Counter.builder("auth.registrations")
                .description("Total number of user registrations")
                .register(meterRegistry);
        
        this.tokenRefreshes = Counter.builder("auth.token.refreshes")
                .description("Total number of token refreshes")
                .register(meterRegistry);
        
        this.loginDuration = Timer.builder("auth.login.duration")
                .description("Login processing time")
                .register(meterRegistry);
    }

    public void incrementLoginAttempts() {
        loginAttempts.increment();
    }

    public void incrementLoginSuccesses() {
        loginSuccesses.increment();
    }

    public void incrementLoginFailures() {
        loginFailures.increment();
    }

    public void incrementRegistrations() {
        registrations.increment();
    }

    public void incrementTokenRefreshes() {
        tokenRefreshes.increment();
    }

    public Timer.Sample startLoginTimer() {
        return Timer.start();
    }

    public void recordLoginDuration(Timer.Sample sample) {
        sample.stop(loginDuration);
    }
}