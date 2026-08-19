package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 元数据（/api/meta 数据源）：活动类型 / 状态 / 来源可信度 / 回归节点定义
 * 每行一个 key，值为 JSON 文本；由 DataSeeder 初始化，管理端可修改。
 */
@Entity
@Table(name = "meta")
@Getter
@Setter
public class Meta {

    /** eventTypes / statuses / sourceLevels / comebackStages */
    @Id
    @Column(length = 32)
    private String metaKey;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String metaValue;
}
