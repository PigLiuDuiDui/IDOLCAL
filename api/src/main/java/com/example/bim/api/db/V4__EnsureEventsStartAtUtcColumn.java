package com.example.bim.api.db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 兜底迁移：更早期的存量库（ddl-auto 时代）events 表没有 start_at_utc 列
 * （该列是"时间换算回填"功能后加的，V1 的 CREATE TABLE IF NOT EXISTS 无法给已存在的旧表补列）。
 * 本迁移幂等补列：已存在（全新库 / 较新存量库）自动跳过。
 * 注意：连接来自 context.getConnection()，归 Flyway 管理，绝不能 close（
 * try-with-resources 关闭会让 Hikari 归还并 reset 连接，Flyway 后续 commit 失败）；
 * 只关闭自己创建的 Statement / ResultSet。
 */
public class V4__EnsureEventsStartAtUtcColumn extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        boolean hasStartAtUtc;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE LOWER(table_name) = 'events' AND LOWER(column_name) = 'start_at_utc'")) {
            rs.next();
            hasStartAtUtc = rs.getInt(1) > 0;
        }
        if (hasStartAtUtc) {
            return; // 全新库：V1 已建；较新存量库：已补过
        }
        // H2 与 PostgreSQL 语法一致：ALTER TABLE ... ADD COLUMN ... BIGINT
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE events ADD COLUMN start_at_utc BIGINT");
        }
    }
}
