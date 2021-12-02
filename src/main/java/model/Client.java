package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Client ORM Model using hibernate
 *
 * @author Patrick Elias
 * @version 2021-12-01
 */

@Entity
@Table(name = "clients")
public class Client implements Serializable {
    @Id @GeneratedValue(generator = "increment")
    private int id;
    private String name;
    private String address;
    private String city;
    private String country;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
