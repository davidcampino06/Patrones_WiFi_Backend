package com.wifisense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wifisense.analysis.AiAnalysisClient;
import com.wifisense.dto.NearbyAnalysisResponse;
import com.wifisense.model.HomeNetwork;
import com.wifisense.model.ScanStatus;
import com.wifisense.model.WifiScanSession;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NearbyAnalysisServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HomeNetworkRepository networks = mock(HomeNetworkRepository.class);
    private final WifiScanSessionRepository scans = mock(WifiScanSessionRepository.class);
    private final WifiScanObservationRepository observations = mock(WifiScanObservationRepository.class);
    private final AiAnalysisClient ai = mock(AiAnalysisClient.class);
    private final NearbyAnalysisService service = new NearbyAnalysisService(networks, scans, observations, ai,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    @Test
    void factsAndInferencesStayApartAndAnInferenceIsNeverCritical() throws Exception {
        NearbyAnalysisResponse answer = NearbyAnalysisService.read(scan(), JSON.readTree("""
                {"facts": [{"code": "CROWDED", "severity": "WARNING", "title": "Canal 6 lleno",
                            "evidence": "7 de 9 redes usan el canal 6."}],
                 "inferences": [{"code": "EVIL_TWIN", "severity": "CRITICAL", "title": "Red parecida a la tuya",
                                 "evidence": "Mismo nombre, otro fabricante.", "confidence": 1.7},
                                {"code": "NO_CONF", "title": "Sin confianza", "evidence": "x"}],
                 "uncertainty": "Un solo escaneo."}"""));

        assertThat(answer.available()).isTrue();
        assertThat(answer.facts()).extracting(NearbyAnalysisResponse.Item::code).containsExactly("CROWDED");
        assertThat(answer.facts().getFirst().confidence()).isNull();
        assertThat(answer.inferences()).hasSize(1);
        assertThat(answer.inferences().getFirst().severity()).isEqualTo("WARNING");
        assertThat(answer.inferences().getFirst().confidence()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(answer.uncertainty()).isEqualTo("Un solo escaneo.");
    }

    @Test
    void withoutACompletedScanNothingIsInvented() {
        owned();
        when(scans.findFirstByNetworkIdAndStatusOrderByFinishedAtDesc(3L, ScanStatus.COMPLETED))
                .thenReturn(Optional.empty());

        NearbyAnalysisResponse answer = service.analyze("ana", 3L);

        assertThat(answer.available()).isFalse();
        assertThat(answer.message()).contains("escanea");
        verifyNoInteractions(ai);
    }

    @Test
    void whenTheAiIsDownTheAnswerSaysSo() {
        owned();
        WifiScanSession scan = scan();
        when(scans.findFirstByNetworkIdAndStatusOrderByFinishedAtDesc(3L, ScanStatus.COMPLETED))
                .thenReturn(Optional.of(scan));
        when(observations.findByScan(11L)).thenReturn(List.of(mock(com.wifisense.model.WifiScanObservation.class,
                RETURNS_DEEP_STUBS)));
        when(ai.nearbyAnalyze(any())).thenThrow(new IllegalStateException("IA apagada"));

        NearbyAnalysisResponse answer = service.analyze("ana", 3L);

        assertThat(answer.available()).isFalse();
        assertThat(answer.scanId()).isEqualTo(11L);
        assertThat(answer.message()).contains("no está disponible");
    }

    @Test
    void anotherUsersNetworkDoesNotExist() {
        when(networks.findByIdAndOwner_Username(3L, "beto")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze("beto", 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private void owned() {
        when(networks.findByIdAndOwner_Username(3L, "ana")).thenReturn(Optional.of(mock(HomeNetwork.class)));
    }

    private static WifiScanSession scan() {
        WifiScanSession scan = mock(WifiScanSession.class);
        when(scan.getId()).thenReturn(11L);
        when(scan.getFinishedAt()).thenReturn(Instant.parse("2026-10-28T15:00:00Z"));
        return scan;
    }
}
