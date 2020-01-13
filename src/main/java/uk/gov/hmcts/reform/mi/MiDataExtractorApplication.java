package uk.gov.hmcts.reform.mi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MiDataExtractorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiDataExtractorApplication.class, args);
	}
}
