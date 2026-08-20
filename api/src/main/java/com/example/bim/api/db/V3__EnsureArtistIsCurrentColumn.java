package com.example.bim.api.db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 兜底迁移：V2 早期版本用大小写敏感查询 information_schema（H2 存大写、PG 存小写），
 * 在 H2 存量库上误判"无 current 列"而漏改名，但已在 flyway_schema_history 记录成功。
 * 本迁移在 V2 之后执行，把漏改名的存量库补上；全新库 / 已正确改名的库自动跳过（幂等）。
 */
public class V3__EnsureArtistIsCurrentColumn extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        V2__RenameArtistCurrentColumn.renameCurrentToIsCurrent(context);
    }
}
