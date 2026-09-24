package com.stakevault.betting.stats.adapter.in.web;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.stakevault.betting.stats.domain.model.BetType;

@Component
class BetTypeQueryParamConverter implements Converter<String, BetType> {

	@Override
	public BetType convert(String source) {
		return BetType.valueOf(source.trim().toUpperCase(Locale.ROOT));
	}
}
