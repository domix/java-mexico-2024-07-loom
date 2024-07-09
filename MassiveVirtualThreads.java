import java.util.Locale;
import java.util.Optional;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.NumberFormat;

public class MassiveVirtualThreads {
    public static void main(String[] args) {
        int threadCount = getFromEnv("THREAD_COUNT", 10_000);
        int size = getFromEnv("SIZE", 1000);
        int maxLatency = getFromEnv("MAX_LATENCY", 100);

        System.out.printf("Se usaran %s threads%n", toHuman(threadCount));
        System.out.printf("La latencia maxima es de %s millis%n", toHuman(maxLatency));

        // Usar un ExecutorService para manejar hilos virtuales
        Instant start;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            start = Instant.now();

            // Crear y ejecutar con los hilos virtuales
            for (int i = 0; i < threadCount; i++) {
                int taskId = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(maxLatency); // Simula una tarea que toma tiempo
                        if (taskId % size == 0) { // Imprimir cada N tareas
                            System.out.printf("Tarea %s completada%n", toHuman(taskId));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.printf("Tarea interrumpida %d%n", taskId);
                    }
                });
            }

            // Cerrar el ExecutorService adecuadamente
            executor.shutdown();
            while (!executor.isTerminated()) {
                // Esperar a que todos los hilos virtuales terminen
            }
        }

        Instant end = Instant.now();
        System.out.printf(
            "Todas las tareas (%s) completadas en: %s milisegundos%n",
            toHuman(threadCount),
            toHuman(Duration.between(start, end).toMillis())
        );

    }

    private static Integer getFromEnv(String name, int defaultValue) {
        return Optional.ofNullable(System.getenv(name))
            .map(Integer::parseInt)
            .orElse(defaultValue);
    }

    public static String toHuman(Number number) {
        return NumberFormat
            .getNumberInstance(Locale.US)
            .format(number);
    }
}
