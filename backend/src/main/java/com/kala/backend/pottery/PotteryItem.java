
package com.kala.backend.pottery;

import jakarta.persistence.*;

@Entity
@Table(name = "pottery_items")
public class PotteryItem {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    protected PotteryItem() {}

    public PotteryItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
