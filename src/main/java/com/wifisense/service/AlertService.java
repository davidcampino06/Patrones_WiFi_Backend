package com.wifisense.service;

import com.wifisense.dto.AlertResponse;
import com.wifisense.model.Alert;
import com.wifisense.repository.AlertRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private final AlertRepository alerts;
    private final Clock clock;

    public AlertService(AlertRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    public List<AlertResponse> list(Alert.Status status, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        List<Alert> result = status == null
                ? alerts.findAllByOrderByCreatedAtDesc(page)
                : alerts.findByStatusOrderByCreatedAtDesc(status, page);
        return result.stream().map(AlertResponse::from).toList();
    }

    @Transactional
    public AlertResponse acknowledge(Long id) {
        Alert alert = find(id);
        alert.acknowledge();
        return AlertResponse.from(alert);
    }

    @Transactional
    public AlertResponse resolve(Long id) {
        Alert alert = find(id);
        alert.resolve(Instant.now(clock));
        return AlertResponse.from(alert);
    }

    private Alert find(Long id) {
        return alerts.findById(id).orElseThrow(() -> new ResourceNotFoundException("la alerta", id));
    }
}
