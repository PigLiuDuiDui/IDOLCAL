package com.example.bim.api.db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 兼容存量开发库：ddl-auto=update 时代 artists 表建的是 current 列，
 * 该列名是 PostgreSQL 保留字（H2 允许），迁移到 Flyway 后统一为 is_current。
 * - 存量库（V1 因 IF NOT EXISTS 跳过已存在的 artists 表）：检测到 current 列存在 → 改名
 * - 全新库（V1 已建 is_current）：无 current 列 → 跳过，幂等
 * 大小写不敏感匹配：H2 未加引号建的表名/列名存储为大写，PostgreSQL 为小写，
 * 写死任何一边都会在另一边误判（V2 早期版本因此漏改名，由 V3 兜底）。
 */
public class V2__RenameArtistCurrentColumn extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        renameCurrentToIsCurrent(context);
    }

    static void renameCurrentToIsCurrent(Context context) throws Exception {
        // 关键：连接来自 context.getConnection()，归 Flyway 管理，绝不能 close（
        // try-with-resources 关闭会让 Hikari 归还并 reset 连接，Flyway 后续 commit/rollback 全部失败）；
        // 只关闭自己创建的 Statement / ResultSet。
        Connection conn = context.getConnection();
        boolean hasCurrentColumn;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.columns " +
                             "WHERE LOWER(table_name) = 'artists' AND LOWER(column_name) = 'current'")) {
            rs.next();
            hasCurrentColumn = rs.getInt(1) > 0;
        }
        if (!hasCurrentColumn) {
            return; // 全新库：V1 已建 is_current
        }
        String db = conn.getMetaData().getDatabaseProductName().toLowerCase();
        try (Statement st = conn.createStatement()) {
            if (db.contains("postgresql")) {
                st.execute("ALTER TABLE artists RENAME COLUMN current TO is_current");
            } else {
                st.execute("ALTER TABLE artists ALTER COLUMN current RENAME TO is_current");
            }
        }
    }
}
