package com.wifisense.controller;

import com.wifisense.dto.DashboardSummary;
import com.wifisense.event.ActivityLogObserver;
import com.wifisense.network.decorator.DataSourceMetrics;
import com.wifisense.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DataSourceMetrics dataSourceMetrics;
    private final ActivityLogObserver activityLog;

    public DashboardController(DashboardService dashboardService, DataSourceMetrics dataSourceMetrics,
                               ActivityLogObserver activityLog) {
        this.dashboardService = dashboardService;
        this.dataSourceMetrics = dataSourceMetrics;
        this.activityLog = activityLog;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/observability/data-sources")
    public List<DataSourceMetrics.Snapshot> dataSources() {
        return dataSourceMetrics.snapshot();
    }

    @GetMapping("/observability/activity")
    public List<ActivityLogObserver.ActivityEntry> activity() {
        return activityLog.recentActivity();
    }
}
