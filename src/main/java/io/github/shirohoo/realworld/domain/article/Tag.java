package io.github.shirohoo.realworld.domain.article;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true, chain = true)
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Article> articles = new HashSet<>();

    public Tag(String name) {
        this.name = name;
    }

    public void tag(Article article) {
        if (this.isTagged(article)) {
            return;
        }

        this.articles.add(article);
        article.addTag(this);
    }

    public boolean isTagged(Article article) {
        return this.articles.contains(article);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tag other && Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
