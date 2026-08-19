package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 打榜 / 数据教程板块（对应前端 src/data/tutorials.js）
 * 嵌套结构（sections[].items[] / notes / noCount）以 JSON 数组字符串存储，
 * 接口层用 JsonNode 透传，保持与前端数据结构一致。
 * status: 'ready' 内容完整可展示；'coming' 待开发（仅占位）。
 */
@Entity
@Table(name = "tutorials")
@Getter
@Setter
public class Tutorial {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 128)
    private String tagline;

    /** sections: [{ title, items: [] }] */
    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String sectionsJson;

    /** notes: [string] */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String notesJson;

    /** noCount: [string] */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String noCountJson;
}
