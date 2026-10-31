package com.wifisense.dto;

import java.util.List;

/** One scan of the history and what it saw (empty when it failed or saw nothing). */
public record WifiScanDetail(WifiScanResponse scan, List<NearbyAccessPoint> accessPoints) {
}
