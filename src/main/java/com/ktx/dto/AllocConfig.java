package com.ktx.dto;

public class AllocConfig {
    private final int policyWeight;
    private final int remoteWeight;
    private final int prevGoodWeight;
    private final String preferenceMode;

    public AllocConfig(int policyWeight, int remoteWeight, int prevGoodWeight, String preferenceMode) {
        this.policyWeight = policyWeight;
        this.remoteWeight = remoteWeight;
        this.prevGoodWeight = prevGoodWeight;
        this.preferenceMode = preferenceMode;
    }

    public int getPolicyWeight() {
        return policyWeight;
    }

    public int getRemoteWeight() {
        return remoteWeight;
    }

    public int getPrevGoodWeight() {
        return prevGoodWeight;
    }

    public String getPreferenceMode() {
        return preferenceMode;
    }

    public boolean softPreference() {
        return "SOFT".equalsIgnoreCase(preferenceMode);
    }
}
