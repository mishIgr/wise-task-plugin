package ru.leti.wise.task.plugin.logic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc.GetPluginResponse;
import ru.leti.wise.task.plugin.mapper.PluginMapper;
import ru.leti.wise.task.plugin.repository.PluginRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetPluginOperation {

    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;

    public GetPluginResponse activate(UUID id) {
        var plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("plugin doesnt exist"));
        return GetPluginResponse.newBuilder()
                .setPlugin(pluginMapper.pluginEntityToPlugin(plugin))
                .build();
    }
}
