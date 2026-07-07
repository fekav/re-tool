package io.fekav.req.review.application;

import io.fekav.platform.cqrs.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ListPendingNodeMatchReviewsQueryHandler
    implements QueryHandler<
        ListPendingNodeMatchReviewsQuery.Result,
        ListPendingNodeMatchReviewsQuery
    > {

    private final NodeMatchReviewProjection reviewProjection;

    @Inject
    public ListPendingNodeMatchReviewsQueryHandler(
        NodeMatchReviewProjection reviewProjection
    ) {
        this.reviewProjection = reviewProjection;
    }

    @Override
    public ListPendingNodeMatchReviewsQuery.Result handle(
        ListPendingNodeMatchReviewsQuery query
    ) {
        return new ListPendingNodeMatchReviewsQuery.Result(
            reviewProjection.pendingReviews()
        );
    }

    @Override
    public Class<ListPendingNodeMatchReviewsQuery> queryType() {
        return ListPendingNodeMatchReviewsQuery.class;
    }
}
