package com.wifisense.dto;

/**
 * A scan the agent must run now. It is the only kind of task the server can give the agent: the agent never runs
 * commands sent by the server, it only decides to scan when it receives one of these.
 */
public record AgentScanTask(Long scanId) {
}
