package com.wifisense.network;

/** Planned: SNMP polling of access points (IF-MIB counters). Not implemented yet. */
public class SnmpDataSource implements NetworkDataSource {

    @Override
    public DataSourceType type() {
        return DataSourceType.SNMP;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        throw new DataSourceUnavailableException("SNMP data source is planned but not implemented yet");
    }
}
