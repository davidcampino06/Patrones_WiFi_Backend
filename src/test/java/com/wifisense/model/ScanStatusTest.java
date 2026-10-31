package com.wifisense.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanStatusTest {

    @Test
    void onlyWaitingAndRunningScansAreActive() {
        assertThat(ScanStatus.PENDING.active()).isTrue();
        assertThat(ScanStatus.RUNNING.active()).isTrue();
        assertThat(ScanStatus.COMPLETED.active()).isFalse();
        assertThat(ScanStatus.FAILED.active()).isFalse();
    }

    @Test
    void aScanMovesForwardAndNeverLeavesAFinishedState() {
        assertThat(ScanStatus.PENDING.canMoveTo(ScanStatus.RUNNING)).isTrue();
        assertThat(ScanStatus.PENDING.canMoveTo(ScanStatus.FAILED)).isTrue();
        assertThat(ScanStatus.RUNNING.canMoveTo(ScanStatus.COMPLETED)).isTrue();
        assertThat(ScanStatus.RUNNING.canMoveTo(ScanStatus.PENDING)).isFalse();
        assertThat(ScanStatus.RUNNING.canMoveTo(ScanStatus.RUNNING)).isFalse();
        for (ScanStatus next : ScanStatus.values()) {
            assertThat(ScanStatus.COMPLETED.canMoveTo(next)).isFalse();
            assertThat(ScanStatus.FAILED.canMoveTo(next)).isFalse();
        }
    }
}
