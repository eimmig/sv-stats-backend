package com.stakevault.betting.stats;

import org.springframework.boot.SpringApplication;

public class TestStatsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(StatsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
