package model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * OrderLine ORM Model using hibernate
 *
 * @author Patrick Elias
 * @version 2021-12-01
 */

@Entity
@Table(name = "order_lines")
public class OrderLine implements Serializable {
    @Id
    @GeneratedValue(generator = "increment")
    private int id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Article article;
    private int amount;

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
