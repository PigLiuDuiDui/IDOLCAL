package com.example.bim.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 活动（对应前端 src/data/events.js 的 events）
 * 多语言字段拆成 en / zh / ko 三列，接口层组装为 { en, 'zh-CN', ko } 对象
 */
@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

    /** 活动 id（e001…），新增时可自动生成 */
    @Id
    @Column(length = 32)
    private String id;

    /** 艺人 id（artists.id） */
    @Column(nullable = false, length = 32)
    private String artist;

    /** 活动日期 YYYY-MM-DD */
    @Column(nullable = false, length = 10)
    private String date;

    /** 跨日结束日期 YYYY-MM-DD（可空） */
    @Column(length = 10)
    private String endDate;

    /** 开始时间 HH:MM；'00:00' 视为全天事件（如生日） */
    @Column(length = 5)
    private String time;

    /** 官方时区缩写（KST / JST / CST…），转换逻辑在前端 utils/time.js */
    @Column(length = 8)
    private String timezone;

    /** 开始时刻 UTC epoch millis（后端计算：date + time + timezone；全天无时刻为 null） */
    @Column(name = "start_at_utc")
    private Long startAtUtc;

    @Column(nullable = false, length = 200)
    private String titleEn;

    @Column(nullable = false, length = 200)
    private String titleZh;

    @Column(nullable = false, length = 200)
    private String titleKo;

    /** 活动类型（meta.eventTypes 定义） */
    @Column(nullable = false, length = 16)
    private String type;

    /** 状态：CONFIRMED / TBA / RUMORED */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 200)
    private String locationEn;

    @Column(length = 200)
    private String locationZh;

    @Column(length = 200)
    private String locationKo;

    @Column(length = 2000)
    private String descriptionEn;

    @Column(length = 2000)
    private String descriptionZh;

    @Column(length = 2000)
    private String descriptionKo;

    @Column(length = 500)
    private String image;

    /** 来源名称（品牌 / 账号名，保持原文不翻译） */
    @Column(length = 100)
    private String sourceName;

    @Column(length = 500)
    private String sourceUrl;

    /** 是否官方来源 */
    @JsonProperty("isOfficial")
    @Column(nullable = false, name = "is_official")
    private boolean isOfficial;

    /** 线上直播地址 */
    @Column(length = 500)
    private String onlineUrl;

    /** 线下地点地图链接 */
    @Column(length = 500)
    private String mapUrl;
}
