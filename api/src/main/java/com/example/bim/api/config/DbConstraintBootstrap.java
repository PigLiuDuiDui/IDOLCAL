package com.example.bim.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库约束补齐：ddl-auto=update 只会给新表建约束，不会修改已存在的表；
 * 启动时幂等补建关键唯一索引（H2 与 PostgreSQL 均支持 IF NOT EXISTS）。
 * 实体上的 @UniqueConstraint 作为契约声明，本组件负责让存量库也生效。
 */
@Component
public class DbConstraintBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbConstraintBootstrap.class);

    private final JdbcTemplate jdbc;

    public DbConstraintBootstrap(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        ensureUniqueIndex("uk_push_tasks_schedule_device",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_push_tasks_schedule_device ON push_tasks (schedule_id, device_id)");
    }

    private void ensureUniqueIndex(String name, String ddl) {
        try {
            jdbc.execute(ddl);
            log.info("[db] 唯一索引已就绪：{}", name);
        } catch (Exception e) {
            // 存量库已有重复数据时无法建索引（仅影响并发兜底，不阻断启动）
            log.warn("[db] 唯一索引 {} 创建失败（存在重复数据？）：{}", name, e.getMessage());
        }
    }
}
