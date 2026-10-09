package com.wifisense.network.decorator;

import java.util.List;

public class InvalidSnapshotException extends RuntimeException {

    public InvalidSnapshotException(List<String> violations) {
        super("Invalid reading from data source: " + String.join("; ", violations));
    }
}
