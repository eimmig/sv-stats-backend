package com.stakevault.betting.stats.adapter.in.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BetEventListener {

	private static final Logger log = LoggerFactory.getLogger(BetEventListener.class);

	private final BetEventSchemaValidator schemaValidator;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public BetEventListener(BetEventSchemaValidator schemaValidator) {
		this.schemaValidator = schemaValidator;
	}

	@RabbitListener(queues = "stats.bet-events")
	public void onMessage(byte[] body) throws IOException {
		JsonNode event = objectMapper.readTree(body);
		String eventType = event.path("eventType").asText();
		schemaValidator.validate(eventType, event);
		if (log.isInfoEnabled()) {
			log.info("received {} event {} for tenant {}", eventType, event.path("eventId").asText(),
					event.path("tenantId").asText());
		}
	}
}
