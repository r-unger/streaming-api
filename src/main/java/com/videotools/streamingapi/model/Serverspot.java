package com.videotools.streamingapi.model;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class Serverspot {
        // @Entity is a JPA annotation to make this object ready
        // for storage in a JPA-based data store

    private String hostname;
    private String group;
    private @Id int token;
        // @Id: primary key

    public Serverspot() {}

    public Serverspot(String hostname, String group, int token) {

        this.hostname = hostname;
        this.group = group;
        this.token = token;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getGroup() {
        return group;
    }

    public int getToken() {
        return this.token;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setToken(int token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof Serverspot))
            return false;
        Serverspot serverspot = (Serverspot) o;
        return Objects.equals(this.hostname, serverspot.hostname)
            && Objects.equals(this.group, serverspot.group)
            && Objects.equals(this.token, serverspot.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hostname, this.group, this.token);
    }

    @Override
    public String toString() {
        return "Serverspot{" 
                + "hostname='" + this.hostname + '\'' 
                + ", group='" + this.group + '\'' 
                + ", token='" + this.token + '\'' + '}';
    }

}
