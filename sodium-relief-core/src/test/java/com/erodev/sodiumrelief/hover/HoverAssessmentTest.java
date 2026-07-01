package com.erodev.sodiumrelief.hover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HoverAssessmentTest {

    @Test
    void flagsAreReportedExactly() {
        HoverAssessment assessment = HoverAssessment.of(true, false, true);
        assertTrue(assessment.skipRedundant());
        assertFalse(assessment.deferHeavyWork());
        assertTrue(assessment.rapidMovement());
    }

    @Test
    void sameFlagsReturnTheSameInternedInstance() {
        assertSame(
            HoverAssessment.of(true, false, true),
            HoverAssessment.of(true, false, true),
            "of(...) must return an interned instance, not allocate per call"
        );
    }

    @Test
    void allEightStatesAreDistinctInstancesWithCorrectFlags() {
        Set<HoverAssessment> distinct = new HashSet<>();
        for (int bits = 0; bits < 8; bits++) {
            boolean skip = (bits & 1) != 0;
            boolean defer = (bits & 2) != 0;
            boolean rapid = (bits & 4) != 0;
            HoverAssessment assessment = HoverAssessment.of(skip, defer, rapid);
            assertEquals(skip, assessment.skipRedundant());
            assertEquals(defer, assessment.deferHeavyWork());
            assertEquals(rapid, assessment.rapidMovement());
            distinct.add(assessment);
        }
        assertEquals(8, distinct.size(), "all eight flag combinations must be distinct instances");
    }
}
