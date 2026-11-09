package cl.bocadopolar.rutas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RutasApplication {

	public static void main(String[] args) {
		SpringApplication.run(RutasApplication.class, args);
	}

}
