package com.limehousehotel.protelapi.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;


@Configuration
@ConfigurationProperties(prefix = "protel.direct-booking")
public class DirectBookingConfig {

    private Set<String> distributionChannels = new HashSet<>();

    public Set<String> getDistributionChannels() {
        return distributionChannels;
    }

    public void setDistributionChannels(Set<String> distributionChannels) {
        this.distributionChannels = distributionChannels;
    }

    public boolean isDirectChannel(String channel) {
        if (channel == null) return false;
        String normalized = channel.trim();
        if (normalized.isEmpty()) return false;

        for (String allowed : distributionChannels) {
            if (allowed != null && allowed.trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
