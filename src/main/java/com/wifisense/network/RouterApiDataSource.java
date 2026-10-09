package com.wifisense.network;

/** Planned: vendor router APIs (each vendor would be wrapped by an Adapter). Not implemented yet. */
public class RouterApiDataSource implements NetworkDataSource {

    @Override
    public DataSourceType type() {
        return DataSourceType.ROUTER_API;
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        throw new DataSourceUnavailableException("La fuente de API de router aún no está disponible");
    }
}
