package com.wifisense.network;

import org.springframework.stereotype.Component;

@Component
public class RouterApiDataSourceCreator extends DataSourceCreator {

    @Override
    public DataSourceType type() {
        return DataSourceType.ROUTER_API;
    }

    @Override
    protected NetworkDataSource createDataSource() {
        return new RouterApiDataSource();
    }
}
