package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stakevault.betting.stats.config.TenantContextHolder;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;

/**
 * Dimensoes ja gravadas no schema da organizacao, para que cada evento nao repita os INSERT ... ON
 * CONFLICT DO NOTHING e as buscas por data e por time (feat-038). Medicao de 2026-11-09: com 20 mil
 * apostas o stats-service fez 35 mil inserts por dimensao para gravar menos de 20 linhas novas.
 *
 * <p>Uma linha so entra aqui depois do commit da transacao que a gravou: se a transacao for desfeita,
 * a dimensao nao existe no banco e nao pode ser dada como conhecida. A chave leva o schema, entao uma
 * organizacao nunca reaproveita a dimensao de outra. As dimensoes nunca sao apagadas nem renomeadas
 * (insertIfAbsent nao atualiza o nome), entao a entrada nao fica desatualizada.
 */
@Component
public class KnownDimensions {

	private static final int MAX_ENTRIES = 200_000;

	private final Map<String, Object> known = new ConcurrentHashMap<>();

	/** Valor guardado para a dimensao, ou vazio se ainda nao confirmada ou sem organizacao no contexto. */
	public <T> Optional<T> find(String kind, String key, Class<T> type) {
		String fullKey = fullKey(kind, key);
		return fullKey == null ? Optional.empty() : Optional.ofNullable(known.get(fullKey)).map(type::cast);
	}

	public boolean contains(String kind, String key) {
		return find(kind, key, Object.class).isPresent();
	}

	/** Registra a dimensao depois do commit da transacao corrente (ou na hora, sem transacao). */
	public void remember(String kind, String key, Object value) {
		String fullKey = fullKey(kind, key);
		if (fullKey == null || value == null) {
			return;
		}
		Runnable put = () -> {
			if (known.size() >= MAX_ENTRIES) {
				known.clear();
			}
			known.put(fullKey, value);
		};
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			put.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				put.run();
			}
		});
	}

	private static String fullKey(String kind, String key) {
		TenantSchemaName schema = TenantContextHolder.current();
		return schema == null || key == null ? null : schema.value() + '|' + kind + '|' + key;
	}
}
