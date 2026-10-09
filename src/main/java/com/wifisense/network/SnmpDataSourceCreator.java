package com.wifisense.network;

import org.springframework.stereotype.Component;

@Component
public class SnmpDataSourceCreator extends DataSourceCreator {

    @Override
    public DataSourceType type() {
        return DataSourceType.SNMP;
    }

    @Override
    protected NetworkDataSource createDataSource() {
        return new SnmpDataSource();
    }
}
