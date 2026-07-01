package com.caredesk.patient.config;

import java.util.UUID;

/**
 * Stable ids for the optional dev test patients seeded by {@link DevTestPatientsSeeder}.
 * Keep in sync with auth-service {@code DevTestPatientsSeeder}.
 */
final class DevTestPatientIds {

    private DevTestPatientIds() {
    }

    static UUID id(int index) {
        return UUID.fromString(String.format("bbbbbbb1-1111-1111-1111-%012d", index));
    }
}
