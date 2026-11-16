package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

/**
 * Quantil bilateral de 95% da distribuicao t de Student, t(0,975; gl), usado no intervalo do ROI
 * (feat-041, ver docs/estatisticas.md). Com poucas apostas a aproximacao normal (z = 1,96)
 * subestima a largura do intervalo; o quantil t corrige isso e converge para z quando gl cresce.
 */
final class StudentT {

	/** t(0,975; gl) para gl = 1..30, tabela padrao com quatro casas. */
	private static final double[] TABLE = { 12.7062, 4.3027, 3.1824, 2.7764, 2.5706, 2.4469, 2.3646, 2.3060,
			2.2622, 2.2281, 2.2010, 2.1788, 2.1604, 2.1448, 2.1314, 2.1199, 2.1098, 2.1009, 2.0930, 2.0860, 2.0796,
			2.0739, 2.0687, 2.0639, 2.0595, 2.0555, 2.0518, 2.0484, 2.0452, 2.0423 };
	private static final double Z = 1.959963984540054;

	private StudentT() {
	}

	/**
	 * t(0,975; gl). Acima de 30 graus de liberdade usa a expansao de Cornish-Fisher do quantil t em
	 * torno do normal (Abramowitz e Stegun, 26.7.5), com erro menor que 1e-4 nessa faixa.
	 */
	static BigDecimal quantile975(long degreesOfFreedom) {
		if (degreesOfFreedom < 1) {
			throw new IllegalArgumentException("degreesOfFreedom must be >= 1");
		}
		if (degreesOfFreedom <= TABLE.length) {
			return BigDecimal.valueOf(TABLE[(int) degreesOfFreedom - 1]);
		}
		double n = degreesOfFreedom;
		double z3 = Z * Z * Z;
		double z5 = z3 * Z * Z;
		double z7 = z5 * Z * Z;
		double g1 = (z3 + Z) / 4;
		double g2 = (5 * z5 + 16 * z3 + 3 * Z) / 96;
		double g3 = (3 * z7 + 19 * z5 + 17 * z3 - 15 * Z) / 384;
		return BigDecimal.valueOf(Z + g1 / n + g2 / (n * n) + g3 / (n * n * n));
	}
}
