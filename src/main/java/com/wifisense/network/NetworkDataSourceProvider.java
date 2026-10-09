package com.wifisense.network;

import com.wifisense.network.decorator.DataSourceMetrics;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Picks the creator registered for a type; new sources only need a new creator bean (OCP). */
@Component
public class NetworkDataSourceProvider {

    private final Map<DataSourceType, DataSourceCreator> creators = new EnumMap<>(DataSourceType.class);
    private final Map<DataSourceType, NetworkDataSource> instances = new ConcurrentHashMap<>();
    private final DataSourceMetrics metrics;

    public NetworkDataSourceProvider(List<DataSourceCreator> creatorBeans, DataSourceMetrics metrics) {
        creatorBeans.forEach(creator -> creators.put(creator.type(), creator));
        this.metrics = metrics;
    }

    public NetworkDataSource forType(DataSourceType type) {
        return instances.computeIfAbsent(type, this::create);
    }

    private NetworkDataSource create(DataSourceType type) {
        DataSourceCreator creator = creators.get(type);
        if (creator == null) {
            throw new DataSourceUnavailableException("No creator registered for " + type);
        }
        return creator.create(metrics);
    }
}
