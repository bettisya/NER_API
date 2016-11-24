package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class NeRdemoApplication{

//	private static final Logger log = LoggerFactory.getLogger(NeRdemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(NeRdemoApplication.class, args);
	}


//	@Override
//	public void run(String... strings) throws Exception {
//		log.info("Creating tables");
////
////		jdbcTemplate.execute("DROP TABLE apiCount IF EXISTS ");
////
//	}
}
