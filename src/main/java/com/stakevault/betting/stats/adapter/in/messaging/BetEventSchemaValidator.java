package com.stakevault.betting.stats.adapter.in.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

@Component
class BetEventSchemaValidator {

	private final Map<String, JsonSchema> schemasByEventType;

	BetEventSchemaValidator() {
		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
		schemasByEventType = Map.of(
				"BetCreated", loadSchema(factory, "/contracts/bet-created.schema.json"),
				"BetSettled", loadSchema(factory, "/contracts/bet-settled.schema.json"));
	}

	private JsonSchema loadSchema(JsonSchemaFactory factory, String resourcePath) {
		try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
			return factory.getSchema(stream);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	void validate(String eventType, JsonNode event) {
		JsonSchema schema = schemasByEventType.get(eventType);
		if (schema == null) {
			throw new UnknownBetEventTypeException(eventType);
		}
		Set<ValidationMessage> errors = schema.validate(event);
		if (!errors.isEmpty()) {
			throw new InvalidBetEventException(eventType, errors);
		}
	}
}
