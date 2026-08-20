package com.example.bim.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL 生产环境迁移验证：在真实 PostgreSQL 容器上执行 Flyway 迁移（V1 建表 + V2/V3 幂等跳过）
 * 并启动完整应用上下文（Hibernate ddl-auto=none + DataSeeder 种子导入），
 * 确认 V1__init.sql 的 PostgreSQL 兼容性（IDENTITY 主键 / TEXT 列 / 唯一索引 / FK 级联）。
 * 无 Docker 环境自动跳过（disabledWithoutDocker = true），不影响本地构建。
 * 运行：mvnw.cmd test -Dtest=PgMigrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PgMigrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void pgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
        registry.add("spring.datasource.driver-class-name", pg::getDriverClassName);
    }

    @Test
    void migrationsApplyAndContextStartsOnPostgresql() {
        // 应用上下文能启动 = V1/V2/V3 迁移成功 + DataSeeder 在 PostgreSQL 上导入种子成功
    }
}
