package io.github.shirohoo.realworld.application.user.service;

import io.github.shirohoo.realworld.domain.user.ProfileVO;
import io.github.shirohoo.realworld.domain.user.User;
import io.github.shirohoo.realworld.domain.user.UserRepository;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileVO getProfile(User me, String target) {
        return userRepository
                .findByUsername(target)
                .map(it -> User.retrievesProfile(me, it))
                .orElseThrow(() -> new NoSuchElementException("User(`%s`) not found".formatted(target)));
    }

    @Transactional(readOnly = true)
    public ProfileVO getProfile(User me, User target) {
        return User.retrievesProfile(me, target);
    }

    @Transactional
    public ProfileVO follow(User me, String target) {
        return userRepository
                .findByUsername(target)
                .map(me::follow)
                .orElseThrow(() -> new NoSuchElementException("User(`%s`) not found".formatted(target)));
    }

    @Transactional
    public ProfileVO follow(User me, User target) {
        return me.follow(target);
    }

    @Transactional
    public ProfileVO unfollow(User me, String target) {
        return userRepository
                .findByUsername(target)
                .map(me::unfollow)
                .orElseThrow(() -> new NoSuchElementException("User(`%s`) not found".formatted(target)));
    }

    @Transactional
    public ProfileVO unfollow(User me, User target) {
        return me.unfollow(target);
    }
}
