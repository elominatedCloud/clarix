package com.clarix.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 신규 약 등록 폼 DTO. 화면은 name1~name5, slots1~slots5 형식으로 보내고,
 * Controller는 rows()로 의미 있는 처방 행만 순회한다.
 */
@Data
public class MedicationSetupForm {
    private String name1;
    private String name2;
    private String name3;
    private String name4;
    private String name5;

    private List<String> slots1 = new ArrayList<>();
    private List<String> slots2 = new ArrayList<>();
    private List<String> slots3 = new ArrayList<>();
    private List<String> slots4 = new ArrayList<>();
    private List<String> slots5 = new ArrayList<>();

    private Integer days1;
    private Integer days2;
    private Integer days3;
    private Integer days4;
    private Integer days5;

    private String storage1;
    private String storage2;
    private String storage3;
    private String storage4;
    private String storage5;

    public List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        addIfPresent(rows, name1, slots1, days1, storage1);
        addIfPresent(rows, name2, slots2, days2, storage2);
        addIfPresent(rows, name3, slots3, days3, storage3);
        addIfPresent(rows, name4, slots4, days4, storage4);
        addIfPresent(rows, name5, slots5, days5, storage5);
        return rows;
    }

    private void addIfPresent(List<Row> rows, String name, List<String> slots,
                              Integer days, String storage) {
        if (name == null || name.isBlank() || slots == null || slots.isEmpty()) return;
        rows.add(new Row(name.trim(), slots, Math.max(1, days == null ? 30 : days), storage));
    }

    public record Row(String name, List<String> slots, int daysSupply, String storage) {}
}
