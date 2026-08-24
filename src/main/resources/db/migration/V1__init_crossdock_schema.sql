CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE warehouse_hubs (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                name VARCHAR(255) NOT NULL,
                                address TEXT NOT NULL,
                                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dock_gates (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            hub_id UUID NOT NULL REFERENCES warehouse_hubs(id) ON DELETE CASCADE,
                            name VARCHAR(50) NOT NULL,
                            gate_type VARCHAR(32) NOT NULL,
                            temperature_mode VARCHAR(32) NOT NULL,
                            created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gate_booking_slots (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    gate_id UUID NOT NULL REFERENCES dock_gates(id),
                                    route_id UUID NOT NULL,
                                    status VARCHAR(32) NOT NULL,
                                    booking_interval TSTZRANGE NOT NULL,
                                    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT no_overlapping_slots EXCLUDE USING GIST (
                                        gate_id WITH =,
                                        booking_interval WITH &&
                                    )WHERE (status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS'))
);

CREATE INDEX idx_gate_booking_slots_gate_id ON gate_booking_slots(gate_id);