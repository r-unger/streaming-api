package com.videotools.streamingapi.model;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class Employee {
        // @Entity is a JPA annotation to make this object ready
        // for storage in a JPA-based data store

    private @Id @GeneratedValue Long id;
        // @Id @GeneratedValue: primary key and automatically populated
        // by the JPA provider
    private String name;
    private String role;

    public Employee() {}

    public Employee(String name, String role) {

        this.name = name;
        this.role = role;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getRole() {
        return this.role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof Employee))
            return false;
        Employee employee = (Employee) o;
        return Objects.equals(this.id, employee.id)
            && Objects.equals(this.name, employee.name)
            && Objects.equals(this.role, employee.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.role);
    }

    @Override
    public String toString() {
        return "Employee{" 
                + "id=" + this.id 
                + ", name='" + this.name + '\'' 
                + ", role='" + this.role + '\'' + '}';
    }
}
