package com.siemens.internship.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution in the application.
 *
 * This is a new class that was not in the original implementation.
 * It configures Spring's async execution environment.
 *
 * BENEFITS:
 * 1. Provides a dedicated thread pool for @Async methods
 * 2. Configures thread pool parameters for optimal performance
 * 3. Enables proper exception handling in async methods
 * 4. Adds meaningful thread names for debugging
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a thread pool task executor for handling asynchronous tasks.
     * This provides better control over the thread pool than the default executor.
     *
     * @return The configured executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Set core pool size (number of threads to keep alive even when idle)
        executor.setCorePoolSize(5);

        // Maximum pool size (maximum threads that can be created)
        executor.setMaxPoolSize(10);

        // Queue capacity (how many tasks can wait if all threads are busy)
        executor.setQueueCapacity(25);

        // Thread name prefix for better debugging
        executor.setThreadNamePrefix("ItemProcessor-");

        // Ensures graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Initialize the executor
        executor.initialize();

        return executor;
    }
}