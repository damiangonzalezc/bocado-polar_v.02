package cl.bocadopolar.frio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FrioApplication {

	public static void main(String[] args) {
		SpringApplication.run(FrioApplication.class, args);
	}

}
