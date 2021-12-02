package model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ORM Model using hibernate.
 *
 * @author Patrick Elias
 * @version 2021-12-01
 */

@Entity
@Table(name="articles")
public class Article implements Serializable {
    @Id @GeneratedValue(generator = "increment")
    private int id;
    private String description;
    private int price;
    private int amount;

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
