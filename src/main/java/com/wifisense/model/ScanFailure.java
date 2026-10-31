package com.wifisense.model;

/**
 * Why a scan failed, as the agent detected it on its computer (or the server, when the agent never answered), with
 * the sentence the page shows. None of them is invented: each one comes from what the system answered.
 */
public enum ScanFailure {
    NO_ADAPTER("El computador del agente no tiene un adaptador Wi-Fi, o el sistema no lo reconoce."),
    ADAPTER_OFF("El Wi-Fi del computador del agente está apagado o desactivado."),
    SERVICE_OFF("El servicio de Wi-Fi de Windows (Configuración automática de WLAN) está detenido."),
    LOCATION_PERMISSION("Windows no deja ver las redes cercanas sin el permiso de ubicación: actívalo en "
            + "Configuración > Privacidad y seguridad > Ubicación, también para las aplicaciones de escritorio."),
    COMMAND_FAILED("El sistema no pudo completar el escaneo."),
    TIMEOUT("El escaneo tardó demasiado en el computador del agente y se canceló."),
    UNREADABLE_OUTPUT("No se pudo entender la respuesta del sistema operativo."),
    UNSUPPORTED_OS("El sistema operativo del agente no permite escanear redes cercanas."),
    AGENT_NO_RESPONSE("El agente no entregó el resultado a tiempo: revisa que el computador esté encendido y el "
            + "agente abierto, y vuelve a intentar.");

    private final String message;

    ScanFailure(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
