package io.fekav.req.review.application;

import java.util.List;
import java.util.Objects;

import io.fekav.platform.cqrs.Query;
import io.fekav.req.review.domain.PendingNodeMatchReview;

public record ListPendingNodeMatchReviewsQuery()
    implements Query<ListPendingNodeMatchReviewsQuery.Result> {

    public record Result(List<PendingNodeMatchReview> reviews) {

        public Result {
            Objects.requireNonNull(reviews, "reviews must not be null");
            if (reviews.stream().anyMatch(Objects::isNull)) {
                throw new NullPointerException("reviews must not contain null");
            }
            reviews = List.copyOf(reviews);
        }
    }
}
