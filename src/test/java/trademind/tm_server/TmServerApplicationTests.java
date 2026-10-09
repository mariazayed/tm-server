package trademind.tm_server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * The whole application, with its real configuration, starts. It needs a scratch PostgreSQL (the same one the
 * other database tests use, see InvestSchemaTest) and is skipped without one.
 *
 * <p>The production database is built by Flyway on top of a base schema that Hibernate created before Flyway
 * was introduced (see the baseline settings in application.properties), so an empty scratch database cannot
 * run the migrations. Here Hibernate builds the schema from the entities instead and Flyway is off; that is
 * enough to prove every bean wires up and the entity mappings are valid. The migrations themselves are
 * checked against the entities by InvestSchemaTest.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@SpringBootTest
@TestPropertySource(properties = {
		"spring.profiles.active=",
		"spring.datasource.url=${INVEST_TEST_DB_URL}",
		"spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
		"spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
		"spring.datasource.hikari.maximum-pool-size=2",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=update"
})
class TmServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
