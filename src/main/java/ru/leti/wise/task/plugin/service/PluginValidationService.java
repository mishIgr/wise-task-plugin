package ru.leti.wise.task.plugin.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.model.Graph;
import ru.leti.wise.task.plugin.Plugin;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.PluginHandler;
import ru.leti.wise.task.plugin.domain.graph.external.ExternalPluginService;
import ru.leti.wise.task.plugin.graph.GraphCharacteristic;
import ru.leti.wise.task.plugin.graph.GraphProperty;
import ru.leti.wise.task.plugin.graph.HandwrittenAnswer;
import ru.leti.wise.task.plugin.graph.NewGraphConstruction;
import ru.leti.wise.task.plugin.mapper.GraphMapper;
import ru.leti.wise.task.plugin.service.grpc.GraphGrpcService;

import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class PluginValidationService {

    @Value("${plugin-validation-limit}")
    private int timeLimit;

    private final GraphGrpcService graphGrpcService;
    private final ExternalPluginService externalPluginService;
    private final PluginHandler graphPluginHandler;
    private final GraphMapper graphMapper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String TEST_HANDWRITTEN_ANSWER = "test";

    public boolean isValidate(PluginEntity pluginEntity, Path path) {
        Graph graph = getGraph();
        var future = executorService.submit(() -> testAbstractPlugin(pluginEntity, graph));
        try {
            future.get(timeLimit, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException e) {
            deleteInvalidPluginFile(path);
            throw new RuntimeException(e); //TODO понятные ошибки
        } catch (TimeoutException e) {
            deleteInvalidPluginFile(path);
            return false;
        }
        deleteInvalidPluginFile(path);
        return true;
    }

    private Graph getGraph() {
        return graphMapper.toGraph(graphGrpcService.getGraph());
    }


    //TODO Доавить проверку типов при добавлении новых типов плагинов
    private String testAbstractPlugin(PluginEntity pluginEntity, Graph graph) throws ConnectException {
        System.out.println(Thread.currentThread().threadId());
        Plugin plugin = externalPluginService.loadPluginFromJar(pluginEntity.getFileName());
        return graphPluginHandler.run(plugin, graph, prepareAdditionData(plugin));
    }

    @SuppressWarnings("unchecked")
    private <T> T prepareAdditionData(Plugin plugin) throws ConnectException {
        return switch (plugin) {
            case GraphProperty p -> null;
            case GraphCharacteristic p -> null;
            case HandwrittenAnswer p -> (T) TEST_HANDWRITTEN_ANSWER;
            case NewGraphConstruction p -> (T) getGraph();
            default -> throw new IllegalStateException("Unexpected value: " + plugin);
        };
    }

    @SneakyThrows
    private void deleteInvalidPluginFile(Path path) {
        Files.delete(path);
    }
}
