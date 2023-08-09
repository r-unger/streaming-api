package com.videotools.streamingapi.model;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class License {
        // @Entity is a JPA annotation to make this object ready
        // for storage in a JPA-based data store

    private @Id int token;
        // @Id: primary key
    private String asset;
    private String status;
    private long leaseTime;
        // TODO: either only renew or pause->onhold, replay->reactivate
    
    public License() {}

    public License(int token, String asset, String status, int leaseTime) {
        this.token = token;
        this.asset = asset;
        this.status = status;
        this.leaseTime = leaseTime;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(long leaseTime) {
        this.leaseTime = leaseTime;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof License))
            return false;
        License license = (License) o;
        return Objects.equals(this.token, license.token)
            && Objects.equals(this.asset, license.asset)
            && Objects.equals(this.status, license.status)
            && Objects.equals(this.leaseTime, license.leaseTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.token, this.asset, this.status, this.leaseTime);
    }

    @Override
    public String toString() {
        return "License{" 
                + "token=" + this.token 
                + ", asset='" + this.asset + '\'' 
                + ", status='" + this.status + '\'' 
                + ", leaseTime='" + this.leaseTime + '\'' + '}';
    }

}
