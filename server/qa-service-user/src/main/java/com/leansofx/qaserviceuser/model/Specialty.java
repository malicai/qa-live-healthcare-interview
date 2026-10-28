package com.leansofx.qaserviceuser.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Specialty {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "name_en", nullable = false, length = 128)
    private String nameEn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Specialty() {
    }

    public Specialty(String name, String nameEn, int sortOrder) {
        this.name = name;
        this.nameEn = nameEn;
        this.sortOrder = sortOrder;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
