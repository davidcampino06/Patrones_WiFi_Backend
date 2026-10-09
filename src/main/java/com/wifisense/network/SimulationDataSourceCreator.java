package com.wifisense.network;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Random;

@Component
public class SimulationDataSourceCreator extends DataSourceCreator {

    private final DataSourceProperties properties;
    private final Clock clock;

    public SimulationDataSourceCreator(DataSourceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public DataSourceType type() {
        return DataSourceType.SIMULATION;
    }

    @Override
    protected NetworkDataSource createDataSource() {
        return new SimulationDataSource(new Random(), properties.simulation().anomalyProbability(), clock);
    }
}
