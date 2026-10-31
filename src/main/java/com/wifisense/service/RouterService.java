package com.wifisense.service;

import com.wifisense.dto.RouterInfoResponse;
import com.wifisense.dto.RouterInfoResponse.Capability;
import com.wifisense.model.NameSource;
import com.wifisense.model.TrackedDevice;
import com.wifisense.repository.AgentDeviceRepository;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.NetworkRepository;
import com.wifisense.repository.SensorDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The router of a network, as the agent sees it: the device it marked as the gateway (role ROUTER) with its maker
 * and model, and how many names the router's own DNS answered. Detecting a network or its router is not managing it:
 * WiFiSense has no official integration to read the router's client list, block a device or change the password, so
 * those are UNAVAILABLE and the answer explains how to do them in the router itself, with the owner's own access.
 */
@Service
public class RouterService {

    static final List<String> STEPS = List.of(
            "Abre en el navegador la dirección del router (suele estar en la etiqueta de abajo: por ejemplo "
                    + "192.168.1.1 o 192.168.0.1) e inicia sesión con la clave de administrador del router.",
            "Busca la sección «Dispositivos conectados», «Lista de clientes» o «DHCP»: ahí está la lista oficial de "
                    + "equipos conectados, con su nombre y su MAC.",
            "Para sacar un equipo que no es tuyo, usa «Control de acceso», «Filtrado MAC» o «Bloquear» en esa misma "
                    + "lista. Si tu router no lo tiene, cambia la contraseña del Wi-Fi: así salen todos y entran solo "
                    + "quienes conozcan la nueva.",
            "La contraseña del Wi-Fi se cambia en «Inalámbrico», «Wi-Fi» o «WLAN» > «Seguridad». Usa WPA2 o WPA3 y "
                    + "una clave larga.",
            "Si el router es del operador (Claro, Movistar, Tigo, ETB...), puede que algunas opciones estén ocultas: "
                    + "pídelas en la línea de soporte del operador.");

    private final HomeNetworkRepository homes;
    private final AgentDeviceRepository homeDevices;
    private final NetworkRepository networks;
    private final SensorDeviceRepository sensorDevices;

    public RouterService(HomeNetworkRepository homes, AgentDeviceRepository homeDevices, NetworkRepository networks,
                         SensorDeviceRepository sensorDevices) {
        this.homes = homes;
        this.homeDevices = homeDevices;
        this.networks = networks;
        this.sensorDevices = sensorDevices;
    }

    /** The router of a Mi Wi-Fi network of this user (another user's network is "not found"). */
    @Transactional(readOnly = true)
    public RouterInfoResponse forHome(String username, Long networkId) {
        homes.findByIdAndOwner_Username(networkId, username)
                .orElseThrow(() -> new ResourceNotFoundException("la red", networkId));
        return describe(homeDevices.findByNetworkIdOrderByFirstSeenAtAsc(networkId));
    }

    /** The router of a team network, as its sensor sees it. */
    @Transactional(readOnly = true)
    public RouterInfoResponse forTeam(Long networkId) {
        if (!networks.existsById(networkId)) {
            throw new ResourceNotFoundException("la red", networkId);
        }
        return describe(sensorDevices.findByNetworkIdOrderByFirstSeenAtAsc(networkId));
    }

    static <D extends TrackedDevice> RouterInfoResponse describe(List<D> devices) {
        Optional<D> router = devices.stream().filter(d -> "ROUTER".equals(d.getRole())).findFirst();
        int fromRouter = (int) devices.stream().filter(d -> d.getNameSource() == NameSource.ROUTER).count();
        int confirmed = (int) devices.stream().filter(d -> d.getNameSource() == NameSource.CONFIRMED).count();
        Capability names = fromRouter + confirmed > 0
                ? new Capability("READ_NAMES", "Leer nombres de los equipos", "AVAILABLE",
                "El DNS del router respondió el nombre de " + (fromRouter + confirmed) + " equipo"
                        + (fromRouter + confirmed == 1 ? "" : "s") + ". Es solo lectura y no usa la clave del router.")
                : new Capability("READ_NAMES", "Leer nombres de los equipos", "UNAVAILABLE",
                "El router no respondió nombres por su DNS (muchos routers del operador no lo hacen), o el agente es "
                        + "anterior a la versión 2.0.0.");
        List<Capability> capabilities = List.of(names,
                new Capability("LIST_CLIENTS", "Lista oficial de clientes del router", "UNAVAILABLE",
                        "WiFiSense no tiene una integración oficial con este router. La lista de Equipos es lo que el "
                                + "agente detecta en la red, no la lista del router."),
                new Capability("BLOCK_DEVICE", "Bloquear un equipo en el router", "UNAVAILABLE",
                        "Marcar un equipo como «no autorizado» en WiFiSense solo te avisa cuando vuelve a "
                                + "conectarse; no lo saca de la red. Para bloquearlo, sigue los pasos de abajo."),
                new Capability("CHANGE_PASSWORD", "Cambiar la contraseña del Wi-Fi", "UNAVAILABLE",
                        "WiFiSense nunca pide ni guarda la clave del router: la contraseña se cambia en el router."));
        return new RouterInfoResponse(router.isPresent(), router.map(TrackedDevice::getVendor).orElse(null),
                router.map(TrackedDevice::getModel).orElse(null), router.map(TrackedDevice::getHostname).orElse(null),
                router.map(TrackedDevice::getLastSeenAt).orElse(null), fromRouter, confirmed, capabilities, STEPS);
    }
}
