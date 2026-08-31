package com.innowise.warehousecrossdock.util;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    public static final PostgreSQLContainer postgresContainer = new PostgreSQLContainer(
            DockerImageName.parse("postgres:15"))
        .withDatabaseName("crossdock");

    @ServiceConnection
    public static final RedisContainer redisContainer = new RedisContainer(
            (DockerImageName.parse("redis:7")))
        .withExposedPorts(6379);

}
