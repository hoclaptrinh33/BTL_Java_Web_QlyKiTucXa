package com.ktx.service;

import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;

public final class ViolationRule {

    private ViolationRule() {
    }

    public static int getDefaultPoints(ViolationType type, ViolationSeverity severity) {
        if (type != null) {
            switch (type) {
                case LATE_RETURN:
                    return 5;
                case ILLEGAL_COOKING:
                    return 20;
                case DISTURBANCE:
                    return 25;
                case DAMAGE:
                    return 50;
                default:
                    break;
            }
        }
        if (severity != null) {
            switch (severity) {
                case MINOR:
                    return 5;
                case MAJOR:
                    return 20;
                case SEVERE:
                    return 50;
            }
        }
        return 5;
    }

    public static ViolationSeverity getDefaultSeverity(ViolationType type) {
        if (type == null) {
            return ViolationSeverity.MINOR;
        }
        switch (type) {
            case LATE_RETURN:
                return ViolationSeverity.MINOR;
            case ILLEGAL_COOKING:
            case DISTURBANCE:
                return ViolationSeverity.MAJOR;
            case DAMAGE:
                return ViolationSeverity.SEVERE;
            default:
                return ViolationSeverity.MINOR;
        }
    }

    public static ViolationAction getDefaultAction(ViolationType type, ViolationSeverity severity, int resultingScore) {
        if ((type == ViolationType.DAMAGE || severity == ViolationSeverity.SEVERE) && resultingScore <= 0) {
            return ViolationAction.TERMINATE;
        }
        if (type == ViolationType.LATE_RETURN) {
            return ViolationAction.WARNING;
        }
        return ViolationAction.POINT_DEDUCT;
    }
}
