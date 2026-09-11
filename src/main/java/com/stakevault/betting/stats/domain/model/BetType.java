package com.stakevault.betting.stats.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BetType {
	@JsonProperty("pre")
	PRE,
	@JsonProperty("live")
	LIVE
}
