package com.caredesk.patient.service;

import java.util.UUID;

public class SlotNotFoundException extends RuntimeException {

    public SlotNotFoundException(UUID slotId) {
        super("Schedule slot not found: " + slotId);
    }
}
