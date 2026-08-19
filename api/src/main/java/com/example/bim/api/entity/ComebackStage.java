package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 回归时间线节点（comebacks.stages[]）
 * 节点 id 引用全局节点定义（meta.comebackStages，如 concept-photo），
 * eventIds 逗号分隔的 events.id 列表（多事件节点如 music-shows 按顺序排列）
 */
@Entity
@Table(name = "comeback_stages")
@Getter
@Setter
public class ComebackStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comeback_id", nullable = false)
    private Comeback comeback;

    /** 节点定义 id（meta.comebackStages[].id） */
    @Column(nullable = false, length = 32)
    private String stageId;

    /** 时间线顺序（0 起） */
    @Column(nullable = false)
    private int sortOrder;

    /** 引用的活动 id，逗号分隔（如 "e014,e015"） */
    @Column(length = 500)
    private String eventIds;
}
