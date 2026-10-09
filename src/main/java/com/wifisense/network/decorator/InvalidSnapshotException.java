package com.wifisense.network.decorator;

import java.util.List;

public class InvalidSnapshotException extends RuntimeException {

    public InvalidSnapshotException(List<String> violations) {
        super("Lectura inválida de la fuente de datos: " + String.join("; ", violations));
    }
}
