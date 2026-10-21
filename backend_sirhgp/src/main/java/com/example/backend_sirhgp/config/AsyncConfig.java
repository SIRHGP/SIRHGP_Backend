package com.example.backend_sirhgp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration de l'exécuteur des méthodes {@code @Async}.
 *
 * ⚠️ Sans cette configuration, {@code @EnableAsync} n'ayant aucun bean
 * {@link TaskExecutor} à disposition, Spring retombe sur
 * {@code SimpleAsyncTaskExecutor} : celui-ci crée un THREAD NEUF par événement,
 * sans aucune borne ni réutilisation (les logs montraient
 * {@code SimpleAsyncTaskExecutor-336} → plus de 336 threads créés).
 *
 * Un pool borné à taille fixe :
 * <ul>
 *   <li>limite le nombre de threads concurrents (donc la pression sur les
 *       connexions JDBC et les sessions Hibernate) ;</li>
 *   <li>réutilise les threads au lieu d'en créer un par événement ;</li>
 *   <li>garantit que les tâches de notification/audit en cours se terminent
 *       proprement à l'arrêt de l'application.</li>
 * </ul>
 *
 * ⚠️ La concurrence reste strictement isolée : chaque tâche asynchrone ouvre sa
 * propre transaction ({@code REQUIRES_NEW}) et ne partage JAMAIS la session
 * Hibernate du thread HTTP.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("sirh-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}