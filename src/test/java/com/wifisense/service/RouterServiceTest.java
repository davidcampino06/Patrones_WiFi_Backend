package com.wifisense.service;

import com.wifisense.dto.RouterInfoResponse;
import com.wifisense.model.NameSource;
import com.wifisense.model.TrackedDevice;
import com.wifisense.repository.AgentDeviceRepository;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.NetworkRepository;
import com.wifisense.repository.SensorDeviceRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RouterServiceTest {

    private final HomeNetworkRepository homes = mock(HomeNetworkRepository.class);
    private final NetworkRepository networks = mock(NetworkRepository.class);
    private final RouterService service = new RouterService(homes, mock(AgentDeviceRepository.class), networks,
            mock(SensorDeviceRepository.class));

    @Test
    void theRouterIsTheGatewayTheAgentMarkedWithItsMakerAndModel() {
        TrackedDevice router = device("ROUTER", NameSource.ANNOUNCED);
        when(router.getVendor()).thenReturn("Huawei");
        when(router.getModel()).thenReturn("HG8145V5");

        RouterInfoResponse info = RouterService.describe(List.of(device(null, NameSource.ROUTER), router,
                device(null, NameSource.ROUTER), device(null, NameSource.DNS)));

        assertThat(info.identified()).isTrue();
        assertThat(info.vendor()).isEqualTo("Huawei");
        assertThat(info.model()).isEqualTo("HG8145V5");
        assertThat(info.namesFromRouter()).isEqualTo(2);
        assertThat(status(info, "READ_NAMES")).isEqualTo("AVAILABLE");
    }

    @Test
    void nothingClaimsAControlOverTheRouterThatDoesNotExist() {
        RouterInfoResponse info = RouterService.describe(List.of(device(null, null)));

        assertThat(info.identified()).isFalse();
        assertThat(status(info, "READ_NAMES")).isEqualTo("UNAVAILABLE");
        assertThat(status(info, "LIST_CLIENTS")).isEqualTo("UNAVAILABLE");
        assertThat(status(info, "BLOCK_DEVICE")).isEqualTo("UNAVAILABLE");
        assertThat(status(info, "CHANGE_PASSWORD")).isEqualTo("UNAVAILABLE");
        assertThat(info.steps()).isNotEmpty().anyMatch(step -> step.contains("Filtrado MAC"));
    }

    @Test
    void theRouterOfAnotherUsersNetworkDoesNotExist() {
        when(homes.findByIdAndOwner_Username(5L, "beto")).thenReturn(Optional.empty());
        when(networks.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.forHome("beto", 5L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.forTeam(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static TrackedDevice device(String role, NameSource source) {
        TrackedDevice device = mock(TrackedDevice.class);
        when(device.getRole()).thenReturn(role);
        when(device.getNameSource()).thenReturn(source);
        when(device.getLastSeenAt()).thenReturn(Instant.parse("2026-10-28T15:00:00Z"));
        return device;
    }

    private static String status(RouterInfoResponse info, String code) {
        return info.capabilities().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow().status();
    }
}
