package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 艺人档案（对应前端 src/data/artists.js）
 * current 标记当前展示的艺人（前端 Hero / 日历订阅使用）
 */
@Entity
@Table(name = "artists")
@Getter
@Setter
public class Artist {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String subName;

    /** 展示年份（Hero 水印）；列名避开 H2 保留字 year */
    @Column(name = "era_year", length = 4)
    private String year;

    /** 当前时期名（如 DEATH OF ME） */
    @Column(length = 64)
    private String era;

    /** 时期区间（如 AUG — NOV 2026） */
    @Column(length = 32)
    private String eraPeriod;

    /** 强调色 */
    @Column(length = 16)
    private String accent;

    @Column(length = 16)
    private String accentSoft;

    /** 官方视觉图片（空则排版式 Hero） */
    @Column(length = 500)
    private String heroImage;

    @Column(length = 32)
    private String sourceTag;

    @Column(length = 2000)
    private String introEn;

    @Column(length = 2000)
    private String introZh;

    @Column(length = 2000)
    private String introKo;

    /** 是否当前展示艺人（日历订阅 / Hero 数据源）；列名 is_current 避开 PostgreSQL 保留字 CURRENT */
    @Column(nullable = false, name = "is_current")
    private boolean current;
}
