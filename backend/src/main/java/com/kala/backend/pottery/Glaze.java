
package com.kala.backend.pottery;

import jakarta.persistence.*;

@Entity
@Table(name = "glazes")
public class Glaze {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Glaze() {}

    public Glaze(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
