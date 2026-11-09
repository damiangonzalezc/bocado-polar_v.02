package cl.bocadopolar.reparto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RepartoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepartoApplication.class, args);
	}

}
