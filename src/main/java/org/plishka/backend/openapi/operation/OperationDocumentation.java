package org.plishka.backend.openapi.operation;

public record OperationDocumentation(
        String operationId,
        boolean secured,
        UnauthorizedMode unauthorizedMode,
        ForbiddenMode forbiddenMode,
        BadRequestDocumentation badRequest,
        String notFoundMessage,
        ConflictExample conflictExample,
        boolean storageUnavailable,
        SuccessExample successExample,
        RequestExample requestExample
) {
    static OperationDocumentation defaults(String operationId) {
        return new OperationDocumentation(
                operationId,
                false,
                UnauthorizedMode.NONE,
                ForbiddenMode.NONE,
                BadRequestDocumentation.none(),
                null,
                null,
                false,
                SuccessExample.NONE,
                RequestExample.NONE
        );
    }

    static Builder builder(String operationId) {
        return new Builder(operationId);
    }

    public boolean hasBadRequest() {
        return badRequest.any();
    }

    public boolean hasNotFound() {
        return notFoundMessage != null;
    }

    public boolean hasConflict() {
        return conflictExample != null;
    }

    static final class Builder {
        private final String operationId;
        private boolean secured;
        private UnauthorizedMode unauthorizedMode = UnauthorizedMode.NONE;
        private ForbiddenMode forbiddenMode = ForbiddenMode.NONE;
        private BadRequestDocumentation badRequest = BadRequestDocumentation.none();
        private String notFoundMessage;
        private ConflictExample conflictExample;
        private boolean storageUnavailable;
        private SuccessExample successExample = SuccessExample.NONE;
        private RequestExample requestExample = RequestExample.NONE;

        private Builder(String operationId) {
            this.operationId = operationId;
        }

        Builder secured() {
            this.secured = true;
            this.unauthorizedMode = UnauthorizedMode.BEARER;
            this.forbiddenMode = ForbiddenMode.ACCESS_DENIED;
            return this;
        }

        Builder shopMode() {
            this.secured = true;
            this.unauthorizedMode = UnauthorizedMode.BEARER;
            this.forbiddenMode = ForbiddenMode.SHOP_MODE;
            return this;
        }

        Builder loginFailureResponses() {
            this.unauthorizedMode = UnauthorizedMode.LOGIN;
            this.forbiddenMode = ForbiddenMode.AUTHENTICATION;
            return this;
        }

        Builder refreshFailureResponses() {
            this.unauthorizedMode = UnauthorizedMode.REFRESH;
            this.forbiddenMode = ForbiddenMode.AUTHENTICATION;
            return this;
        }

        Builder badRequest(BadRequestDocumentation badRequest) {
            this.badRequest = badRequest;
            return this;
        }

        Builder notFound(String notFoundMessage) {
            this.notFoundMessage = notFoundMessage;
            return this;
        }

        Builder conflict(ConflictExample conflictExample) {
            this.conflictExample = conflictExample;
            return this;
        }

        Builder storageUnavailable() {
            this.storageUnavailable = true;
            return this;
        }

        Builder successExample(SuccessExample successExample) {
            this.successExample = successExample;
            return this;
        }

        Builder requestExample(RequestExample requestExample) {
            this.requestExample = requestExample;
            return this;
        }

        OperationDocumentation build() {
            return new OperationDocumentation(
                    operationId,
                    secured,
                    unauthorizedMode,
                    forbiddenMode,
                    badRequest,
                    notFoundMessage,
                    conflictExample,
                    storageUnavailable,
                    successExample,
                    requestExample
            );
        }
    }
}
