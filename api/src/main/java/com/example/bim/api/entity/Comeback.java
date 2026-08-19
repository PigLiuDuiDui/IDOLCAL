package com.example.bim.api.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 回归专题（对应前端 src/data/comebacks.js）
 * stages 为时间线节点（一对多），节点只引用 events 表中的活动，不重复创建数据
 */
@Entity
@Table(name = "comebacks")
@Getter
@Setter
public class Comeback {

    @Id
    @Column(length = 32)
    private String id;

    /** 艺人 id（artists.id） */
    @Column(nullable = false, length = 32)
    private String artistId;

    /** 回归名（如 DEATH OF ME），单语 */
    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 64)
    private String taglineEn;

    @Column(length = 64)
    private String taglineZh;

    @Column(length = 64)
    private String taglineKo;

    /** 发行日 YYYY-MM-DD（D-Day 按官方时区计算） */
    @Column(nullable = false, length = 10)
    private String releaseDate;

    /** 发行时间 HH:MM */
    @Column(nullable = false, length = 5)
    private String releaseTime;

    /** 发行时区（KST） */
    @Column(nullable = false, length = 8)
    private String releaseTimezone;

    /** 时间线节点，按 sortOrder 升序 */
    @OneToMany(mappedBy = "comeback", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ComebackStage> stages = new ArrayList<>();
}
