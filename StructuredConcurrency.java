import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

public class StructuredConcurrency {
    static AtomicInteger taskCounter = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws InterruptedException {
        int threadCount = getFromEnv("THREAD_COUNT", 10_000);
        int size = getFromEnv("SIZE", 1000);
        int maxLatency = getFromEnv("MAX_LATENCY", 100);
        boolean canFail = System.getenv("CAN_FAIL") != null;

        final var structuredTaskScope = getStructuredTaskScope();
        System.out.println(structuredTaskScope.getClass().getSimpleName());

        System.out.printf("Se usaran %s threads%n", toHuman(threadCount));
        System.out.printf("La latencia maxima es de %s millis%n", toHuman(maxLatency));

        //new StructuredTaskScope.ShutdownOnSuccess
        // Crea un Scope para la concurrencia estructurada
        try (var scope = structuredTaskScope) {
            for (int i = 0; i < threadCount; i++) {
                int finalI = i;
                // Envía tareas para ejecución concurrente
                scope.fork(() -> {
                    // Simula una tarea que toma tiempo
                    processTask(finalI, maxLatency, size, canFail);
                    return finalI;
                });
            }

            // Espera a que todas las tareas se completen o una falle
            scope.join();
            if (structuredTaskScope instanceof StructuredTaskScope.ShutdownOnFailure shutdownOnFailure) {
                shutdownOnFailure.throwIfFailed();  // Lanza una excepción si alguna tarea falló
            }
            System.out.println("Todas las tareas han completado con éxito");

        } catch (ExecutionException e) {
            System.err.println("Una o más tareas fallaron: " + e.getMessage());
        }
        finally {
            System.out.println("Tareas ejecutadas " + taskCounter.get());
        }

    }

    // Simula el procesamiento de una tarea
    private static void processTask(int taskId, int maxLatency, int size, boolean canFail) {
        try {
            // Simulación de una tarea que podría ser costosa, como procesamiento de datos
            Thread.sleep((long) (Math.random() * maxLatency));
            if (canFail && Math.random() < 0.01) {
                throw new RuntimeException("Error en la tarea " + taskId);
            }
            if (taskId % size == 0) {
                System.out.println("Tarea " + taskId + " completada");
            }
            taskCounter.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Tarea interrumpida " + taskId);
        }
    }

    private static StructuredTaskScope getStructuredTaskScope() {
        return Optional.ofNullable(System.getenv("FAILURE_SCOPE"))
            .map(_ -> createShutdownOnFailureScope())
            .orElse(createShutdownOnSuccessScope());
    }

    private static StructuredTaskScope createShutdownOnFailureScope() {
        return new StructuredTaskScope.ShutdownOnFailure();
    }

    private static StructuredTaskScope createShutdownOnSuccessScope() {
        return new StructuredTaskScope.ShutdownOnSuccess();
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