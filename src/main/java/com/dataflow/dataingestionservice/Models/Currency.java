package com.dataflow.dataingestionservice.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Table(name = "currencies")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    @Id
    @Column(name = "id",length = 36, nullable = false)
    private String id;

    @Column(nullable = false, name = "code",unique = true)
    private String code;

    @Column(nullable = true,name = "name")
    private String name;

    @Override
    public String toString() {
        return "Currency{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getIdAsString(){
        return id.toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
