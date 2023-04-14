package io.github.shirohoo.realworld.application.content;

import io.github.shirohoo.realworld.domain.content.Article;
import io.github.shirohoo.realworld.domain.content.ArticleFacets;
import io.github.shirohoo.realworld.domain.content.ArticleRepository;
import io.github.shirohoo.realworld.domain.content.ArticleVO;
import io.github.shirohoo.realworld.domain.content.Comment;
import io.github.shirohoo.realworld.domain.content.CommentRepository;
import io.github.shirohoo.realworld.domain.content.CommentVO;
import io.github.shirohoo.realworld.domain.content.TagRepository;
import io.github.shirohoo.realworld.domain.user.User;

import java.util.*;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public ArticleVO getSingleArticle(User me, String slug) {
        return articleRepository
                .findBySlug(slug)
                .map(article -> new ArticleVO(me, article))
                .orElseThrow(() -> new NoSuchElementException("Article not found: `%s`".formatted(slug)));
    }

    @Transactional(readOnly = true)
    public List<ArticleVO> getArticles(User me, ArticleFacets facets) {
        String tag = facets.tag();
        String author = facets.author();
        String favorited = facets.favorited();
        Pageable pageable = facets.getPageable();

        return articleRepository.findByFacets(tag, author, favorited, pageable).getContent().stream()
                .map(article -> new ArticleVO(me, article))
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public List<ArticleVO> getFeedArticles(User me, ArticleFacets facets) {
        Set<User> followings = me.followings();
        Pageable pageable = facets.getPageable();

        return articleRepository
                .findByAuthorInOrderByCreatedAtDesc(followings, pageable)
                .map(article -> new ArticleVO(me, article))
                .getContent();
    }

    @Transactional
    public ArticleVO createArticle(User me, CreateArticleRequest request) {
        Article newArticle = Article.builder()
                .author(me)
                .title(request.title())
                .description(request.description())
                .content(request.body())
                .tags(new HashSet<>(tagRepository.saveAll(request.tags())))
                .build();

        newArticle = articleRepository.save(newArticle);
        return new ArticleVO(me, newArticle);
    }

    @Transactional
    public ArticleVO updateArticle(User me, String slug, UpdateArticleRequest request) {
        return articleRepository
                .findBySlug(slug)
                .map(it -> it.update(me, request.title(), request.description(), request.body()))
                .map(it -> new ArticleVO(me, articleRepository.save(it)))
                .orElseThrow(() -> new NoSuchElementException("Article not found by slug: `%s`".formatted(slug)));
    }

    @Transactional
    public void deleteArticle(User me, String slug) {
        articleRepository
                .findBySlug(slug)
                .ifPresentOrElse(
                        article -> {
                            if (article.isAuthoredBy(me)) articleRepository.delete(article);
                            else throw new IllegalArgumentException("You cannot delete articles written by others.");
                        },
                        () -> {
                            throw new NoSuchElementException("Article not found by slug: `%s`".formatted(slug));
                        });
    }

    @Transactional
    public CommentVO createComment(User me, String slug, CreateCommentRequest request) {
        return articleRepository
                .findBySlug(slug)
                .map(article -> Comment.builder()
                        .author(me)
                        .article(article)
                        .content(request.body())
                        .build())
                .map(commentRepository::save)
                .map(c -> new CommentVO(me, c))
                .orElseThrow(() -> new NoSuchElementException("Article not found by slug: `%s`".formatted(slug)));
    }

    @Transactional
    public List<CommentVO> getArticleComments(User me, String slug) {
        return articleRepository
                .findBySlug(slug)
                .map(commentRepository::findByArticleOrderByCreatedAtDesc)
                .orElseThrow(() -> new NoSuchElementException("Article not found by slug: `%s`".formatted(slug)))
                .stream()
                .map(comment -> new CommentVO(me, comment))
                .toList();
    }

    @Transactional
    public void deleteComment(User me, int commentId) {
        commentRepository
                .findById(commentId)
                .ifPresentOrElse(
                        comment -> {
                            if (comment.isAuthoredBy(me)) commentRepository.delete(comment);
                            else throw new IllegalArgumentException("You cannot delete comments written by others.");
                        },
                        () -> {
                            throw new NoSuchElementException("Comment not found by id: `%d`".formatted(commentId));
                        });
    }

    @Transactional
    public ArticleVO favoriteArticle(User me, String slug) {
        return articleRepository
                .findBySlug(slug)
                .map(article -> new ArticleVO(me, article.favoritedBy(me)))
                .orElseThrow(() -> new NoSuchElementException("Article not found by slug: `%s`".formatted(slug)));
    }

    @Transactional
    public ArticleVO unfavoriteArticle(User me, String slug) {
        return articleRepository
                .findBySlug(slug)
                .map(article -> new ArticleVO(me, article.unfavoritedBy(me)))
                .orElseThrow(() -> new NoSuchElementException("Article not found by slug: `%s`".formatted(slug)));
    }
}
