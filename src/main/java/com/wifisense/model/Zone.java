package com.wifisense.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "zones")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, length = 100)
    private String name;

    private Integer floor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Zone() {
    }

    public Zone(Location location, String name, Integer floor) {
        this.location = location;
        this.name = name;
        this.floor = floor;
    }

    public Long getId() { return id; }
    public Location getLocation() { return location; }
    public String getName() { return name; }
    public Integer getFloor() { return floor; }
}
