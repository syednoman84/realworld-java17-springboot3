package io.github.shirohoo.realworld.application.config;

import java.io.IOException;
import java.util.NoSuchElementException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionHandleFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException {
        try {
            filterChain.doFilter(request, response);

        } catch (IllegalArgumentException e) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
            log.info("Illegal argument: `{}`", e.getMessage());

            response.setStatus(problemDetail.getStatus());
            response.setContentType("application/problem+json");
            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));

        } catch (NoSuchElementException e) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            log.info("No such element: `{}`", e.getMessage());

            response.setStatus(problemDetail.getStatus());
            response.setContentType("application/problem+json");
            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));

        } catch (Exception e) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR, "An error has occurred. Please contact the administrator.");
            log.error("An unknown error occurred: `{}`. Please contact the administrator.", e.getMessage(), e);

            response.setStatus(problemDetail.getStatus());
            response.setContentType("application/problem+json");
            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
        }
    }
}
