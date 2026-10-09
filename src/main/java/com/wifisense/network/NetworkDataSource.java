package com.wifisense.network;

/** Product of the Factory Method and Component of the Decorator chain. */
public interface NetworkDataSource {

    DataSourceType type();

    NetworkSnapshot collect(NetworkTarget target);
}
