package com.wifisense.network.decorator;

import com.wifisense.network.DataSourceType;
import com.wifisense.network.NetworkDataSource;
import com.wifisense.network.NetworkSnapshot;
import com.wifisense.network.NetworkTarget;

/** Base Decorator: wraps any data source and forwards calls by default. */
public abstract class DataSourceDecorator implements NetworkDataSource {

    protected final NetworkDataSource delegate;

    protected DataSourceDecorator(NetworkDataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public DataSourceType type() {
        return delegate.type();
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        return delegate.collect(target);
    }
}
