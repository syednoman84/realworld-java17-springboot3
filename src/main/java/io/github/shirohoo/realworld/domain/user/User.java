package io.github.shirohoo.realworld.domain.user;

import io.github.shirohoo.realworld.domain.article.Article;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.*;
import lombok.experimental.Accessors;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Accessors(fluent = true, chain = true)
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @Setter(AccessLevel.PRIVATE)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String username;

    private String bio;

    private String image;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder.Default
    @Setter(AccessLevel.PRIVATE)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "users_follow",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id"))
    private Set<User> followings = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "followings", cascade = CascadeType.ALL)
    private final Set<User> followers = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "favorites", cascade = CascadeType.ALL)
    private final Set<Article> favoritedArticles = new HashSet<>();

    @Transient
    private String token;

    public static ProfileVO retrievesProfile(User me, User target) {
        if (me == null) {
            return new ProfileVO(null, target);
        }

        return new ProfileVO(me, target);
    }

    public ProfileVO follow(User target) {
        if (this.followings.contains(target)) {
            return new ProfileVO(this, target);
        }

        this.followings.add(target);
        target.followers.add(this);

        return User.retrievesProfile(this, target);
    }

    public ProfileVO unfollow(User target) {
        if (!this.followings.contains(target)) {
            return new ProfileVO(this, target);
        }

        this.followings.remove(target);
        target.followers.remove(this);

        return User.retrievesProfile(this, target);
    }

    public boolean isFollowing(User target) {
        return this.followings.contains(target);
    }

    public void favorite(Article article) {
        if (this.favoritedArticles.contains(article)) {
            return;
        }

        this.favoritedArticles.add(article);
        article.favoritedBy(this);
    }

    public void unfavorite(Article article) {
        if (!this.favoritedArticles.contains(article)) {
            return;
        }

        this.favoritedArticles.remove(article);
        article.unfavoritedBy(this);
    }

    public Set<User> followings() {
        return Set.copyOf(this.followings);
    }

    public Set<User> followers() {
        return Set.copyOf(this.followers);
    }

    public Set<Article> favoritedArticles() {
        return Set.copyOf(this.favoritedArticles);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
