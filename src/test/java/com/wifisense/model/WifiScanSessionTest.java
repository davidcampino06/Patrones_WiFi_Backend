package com.wifisense.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WifiScanSessionTest {

    private static final Instant ASKED = Instant.parse("2026-10-26T20:00:00Z");
    private static final WifiScanSession.ScanDetails NETSH =
            new WifiScanSession.ScanDetails("Wi-Fi", "NETSH", true, 4200, "1.9.0");

    private final WifiScanSession scan = new WifiScanSession(mock(HomeNetwork.class), ScanOrigin.MANUAL, 3L, ASKED);

    @Test
    void aNewScanWaitsForTheAgent() {
        assertThat(scan.getStatus()).isEqualTo(ScanStatus.PENDING);
        assertThat(scan.getRequestedBy()).isEqualTo(3L);
        assertThat(scan.getStartedAt()).isNull();
    }

    @Test
    void aCompletedScanKeepsHowItWasDoneAndWhatItCounted() {
        scan.start(ASKED.plusSeconds(20));
        scan.complete(ASKED.plusSeconds(25), NETSH, 4, 6);

        assertThat(scan.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(scan.getStartedAt()).isEqualTo(ASKED.plusSeconds(20));
        assertThat(scan.getFinishedAt()).isEqualTo(ASKED.plusSeconds(25));
        assertThat(scan.getNetworkCount()).isEqualTo(4);
        assertThat(scan.getAccessPointCount()).isEqualTo(6);
        assertThat(scan.getInterfaceName()).isEqualTo("Wi-Fi");
        assertThat(scan.getScanTool()).isEqualTo("NETSH");
        assertThat(scan.getFresh()).isTrue();
        assertThat(scan.getFailureReason()).isNull();
    }

    @Test
    void zeroNetworksIsAValidResult() {
        scan.complete(ASKED.plusSeconds(5), NETSH, 0, 0);

        assertThat(scan.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(scan.getNetworkCount()).isZero();
        assertThat(scan.getStartedAt()).isEqualTo(scan.getFinishedAt());
    }

    @Test
    void aFailedScanSaysWhy() {
        scan.start(ASKED.plusSeconds(10));
        scan.fail(ASKED.plusSeconds(12), ScanFailure.LOCATION_PERMISSION, NETSH);

        assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(scan.getFailureReason()).isEqualTo(ScanFailure.LOCATION_PERMISSION);
        assertThat(scan.getFailureReason().message()).contains("permiso de ubicación");
        assertThat(scan.getNetworkCount()).isNull();
    }

    @Test
    void aFinishedScanNeverChangesAgain() {
        scan.fail(ASKED.plusSeconds(200), ScanFailure.AGENT_NO_RESPONSE, null);

        assertThatThrownBy(() -> scan.complete(ASKED.plusSeconds(201), NETSH, 3, 3))
                .isInstanceOf(IllegalStateException.class).hasMessage("Ese escaneo ya terminó.");
        assertThatThrownBy(() -> scan.start(ASKED.plusSeconds(201))).isInstanceOf(IllegalStateException.class);
        assertThat(scan.getFailureReason()).isEqualTo(ScanFailure.AGENT_NO_RESPONSE);
    }

    @Test
    void aClockThatWentBackNeverDatesTheScanBeforeItWasAskedFor() {
        scan.start(ASKED.minusSeconds(30));
        scan.complete(ASKED.minusSeconds(20), NETSH, 1, 1);

        assertThat(scan.getStartedAt()).isEqualTo(ASKED);
        assertThat(scan.getFinishedAt()).isEqualTo(ASKED);
    }

    @Test
    void everyFailureHasASentenceForThePage() {
        for (ScanFailure failure : ScanFailure.values()) {
            assertThat(failure.message()).isNotBlank().endsWith(".");
        }
    }
}
