package com.taskmanagement.analytics.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdServiceTest {

    private CorrelationIdService correlationIdService;

    @BeforeEach
    void setUp() {
        correlationIdService = new CorrelationIdService();
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    void testGenerateCorrelationId() {
        // Generate correlation ID
        String correlationId = correlationIdService.generateCorrelationId();

        // Verify it's not null and has expected format (UUID)
        assertNotNull(correlationId);
        assertFalse(correlationId.trim().isEmpty());
        assertTrue(correlationId.contains("-")); // UUID format
        assertEquals(36, correlationId.length()); // Standard UUID length
    }

    @Test
    void testSetAndGetCorrelationId() {
        String testId = "test-correlation-id-123";

        // Initially should be null
        assertNull(correlationIdService.getCorrelationId());

        // Set correlation ID
        correlationIdService.setCorrelationId(testId);

        // Verify it was set
        assertEquals(testId, correlationIdService.getCorrelationId());

        // Verify it's also in MDC
        assertEquals(testId, MDC.get(CorrelationIdService.CORRELATION_ID_KEY));
    }

    @Test
    void testSetCorrelationIdWithNullValue() {
        // Set a valid ID first
        correlationIdService.setCorrelationId("valid-id");
        assertEquals("valid-id", correlationIdService.getCorrelationId());

        // Try to set null - should not change the existing value
        correlationIdService.setCorrelationId(null);
        assertEquals("valid-id", correlationIdService.getCorrelationId());
    }

    @Test
    void testSetCorrelationIdWithEmptyValue() {
        // Set a valid ID first
        correlationIdService.setCorrelationId("valid-id");
        assertEquals("valid-id", correlationIdService.getCorrelationId());

        // Try to set empty string - should not change the existing value
        correlationIdService.setCorrelationId("");
        assertEquals("valid-id", correlationIdService.getCorrelationId());

        // Try to set whitespace - should not change the existing value
        correlationIdService.setCorrelationId("   ");
        assertEquals("valid-id", correlationIdService.getCorrelationId());
    }

    @Test
    void testClearCorrelationId() {
        String testId = "test-correlation-id-456";

        // Set correlation ID
        correlationIdService.setCorrelationId(testId);
        assertEquals(testId, correlationIdService.getCorrelationId());

        // Clear correlation ID
        correlationIdService.clearCorrelationId();

        // Verify it was cleared
        assertNull(correlationIdService.getCorrelationId());
        assertNull(MDC.get(CorrelationIdService.CORRELATION_ID_KEY));
    }

    @Test
    void testClearCorrelationIdWhenNotSet() {
        // Initially should be null
        assertNull(correlationIdService.getCorrelationId());

        // Clear should not throw exception
        assertDoesNotThrow(() -> correlationIdService.clearCorrelationId());

        // Should still be null
        assertNull(correlationIdService.getCorrelationId());
    }

    @Test
    void testGetOrGenerateCorrelationId() {
        // Initially should generate new ID
        String generatedId = correlationIdService.getOrGenerateCorrelationId();
        assertNotNull(generatedId);
        assertFalse(generatedId.trim().isEmpty());

        // Calling again should return the same ID
        String sameId = correlationIdService.getOrGenerateCorrelationId();
        assertEquals(generatedId, sameId);

        // Clear and call again should generate new ID
        correlationIdService.clearCorrelationId();
        String newId = correlationIdService.getOrGenerateCorrelationId();
        assertNotNull(newId);
        assertNotEquals(generatedId, newId);
    }

    @Test
    void testExecuteWithCorrelationId() {
        String testId = "test-execution-id";
        AtomicReference<String> capturedId = new AtomicReference<>();

        // Execute runnable with correlation ID
        correlationIdService.executeWithCorrelationId(testId, () -> {
            capturedId.set(correlationIdService.getCorrelationId());
        });

        // Verify the correlation ID was available during execution
        assertEquals(testId, capturedId.get());

        // Verify correlation ID is cleared after execution
        assertNull(correlationIdService.getCorrelationId());
    }

    @Test
    void testExecuteWithCorrelationIdPreservesExisting() {
        String existingId = "existing-id";
        String executionId = "execution-id";

        // Set existing correlation ID
        correlationIdService.setCorrelationId(existingId);

        AtomicReference<String> capturedId = new AtomicReference<>();

        // Execute runnable with different correlation ID
        correlationIdService.executeWithCorrelationId(executionId, () -> {
            capturedId.set(correlationIdService.getCorrelationId());
        });

        // Verify the execution ID was used during execution
        assertEquals(executionId, capturedId.get());

        // Verify existing ID is restored after execution
        assertEquals(existingId, correlationIdService.getCorrelationId());
    }

    @Test
    void testExecuteWithCorrelationIdHandlesException() {
        String testId = "test-exception-id";
        String existingId = "existing-id";

        // Set existing correlation ID
        correlationIdService.setCorrelationId(existingId);

        // Execute runnable that throws exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            correlationIdService.executeWithCorrelationId(testId, () -> {
                throw new RuntimeException("Test exception");
            });
        });

        assertEquals("Test exception", exception.getMessage());

        // Verify existing ID is restored even after exception
        assertEquals(existingId, correlationIdService.getCorrelationId());
    }

    @Test
    void testThreadIsolation() throws ExecutionException, InterruptedException {
        // Test that correlation IDs are isolated between threads
        String mainThreadId = "main-thread-id";
        String otherThreadId = "other-thread-id";

        // Set correlation ID in main thread
        correlationIdService.setCorrelationId(mainThreadId);

        // Execute in another thread
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // Should be null in new thread
            String initialId = correlationIdService.getCorrelationId();
            
            // Set different ID in this thread
            correlationIdService.setCorrelationId(otherThreadId);
            String setId = correlationIdService.getCorrelationId();
            
            return initialId + ":" + setId;
        });

        String result = future.get();
        String[] parts = result.split(":");

        // Verify other thread started with null and set its own ID
        assertEquals("null", parts[0]);
        assertEquals(otherThreadId, parts[1]);

        // Verify main thread still has its original ID
        assertEquals(mainThreadId, correlationIdService.getCorrelationId());
    }

    @Test
    void testMultipleGeneratedIdsAreUnique() {
        // Generate multiple correlation IDs
        String id1 = correlationIdService.generateCorrelationId();
        String id2 = correlationIdService.generateCorrelationId();
        String id3 = correlationIdService.generateCorrelationId();

        // Verify they are all different
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);

        // Verify they all have UUID format
        assertTrue(id1.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertTrue(id2.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertTrue(id3.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
}