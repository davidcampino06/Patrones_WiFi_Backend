package com.wifisense.controller;

import com.wifisense.dto.MeasurementResponse;
import com.wifisense.dto.ProtocolStatisticResponse;
import com.wifisense.dto.TrafficObservationResponse;
import com.wifisense.service.MeasurementCollectionService;
import com.wifisense.service.MeasurementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/networks/{networkId}")
public class MeasurementController {

    private final MeasurementService measurementService;
    private final MeasurementCollectionService collectionService;

    public MeasurementController(MeasurementService measurementService,
                                 MeasurementCollectionService collectionService) {
        this.measurementService = measurementService;
        this.collectionService = collectionService;
    }

    @GetMapping("/measurements")
    public List<MeasurementResponse> history(
            @PathVariable Long networkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "200") int limit) {
        return measurementService.history(networkId, from, to, limit);
    }

    @PostMapping("/measurements/collect")
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse collect(@PathVariable Long networkId) {
        return collectionService.collect(networkId);
    }

    @GetMapping("/traffic")
    public List<TrafficObservationResponse> traffic(@PathVariable Long networkId,
                                                    @RequestParam(defaultValue = "96") int limit) {
        return measurementService.traffic(networkId, limit);
    }

    @GetMapping("/protocols")
    public List<ProtocolStatisticResponse> protocols(@PathVariable Long networkId) {
        return measurementService.latestProtocols(networkId);
    }
}
