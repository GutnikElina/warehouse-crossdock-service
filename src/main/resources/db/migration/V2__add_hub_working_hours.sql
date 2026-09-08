ALTER TABLE warehouse_hubs
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
ADD COLUMN working_hours_start TIME NOT NULL DEFAULT '00:00:00',
ADD COLUMN working_hours_end TIME NOT NULL DEFAULT '23:59:59';

CREATE INDEX idx_dock_gates_search ON dock_gates(hub_id, gate_type, temperature_mode);