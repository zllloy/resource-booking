package com.ramil.booking.resource_booking.api.graphql.error;

import java.util.Map;
import java.util.concurrent.CompletionException;

import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.CoercingParseValueException;
import graphql.schema.DataFetchingEnvironment;

import com.ramil.booking.resource_booking.domain.booking.exception.BookingAccessDeniedException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingConflictException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingNotFoundException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingStatusException;
import com.ramil.booking.resource_booking.domain.booking.exception.BookingTimeRangeException;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceInactiveException;
import com.ramil.booking.resource_booking.domain.resource.exception.ResourceNotFoundException;
import com.ramil.booking.resource_booking.domain.user.exception.UserNotFoundException;
import com.ramil.booking.resource_booking.domain.payment.exception.PaymentAccessDeniedException;


@Component
@Order(-2)
public class GraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        Throwable e = unwrap(ex);

        // права
        if (e instanceof AccessDeniedException || e instanceof BookingAccessDeniedException) {
            return err(env, "У вас недостаточно прав для выполнения этой операции",
                    ErrorType.FORBIDDEN, "INSUFFICIENT_PERMISSIONS");
        }

        if (isInvalidUuid(e)) {
            return err(env, "Некорректный UUID. Передайте корректный id в формате UUID.",
                    ErrorType.BAD_REQUEST, "INVALID_UUID");
        }

        if (e instanceof BookingNotFoundException) {
            return err(env, "Бронирование не найдено", ErrorType.NOT_FOUND, "BOOKING_NOT_FOUND");
        }
        if (e instanceof BookingConflictException) {
            return err(env, "Бронирование пересекается с существующим",
                    ErrorType.BAD_REQUEST, "BOOKING_CONFLICT");
        }
        if (e instanceof BookingTimeRangeException) {
            return err(env, e.getMessage(), ErrorType.BAD_REQUEST, "INVALID_TIME_RANGE");
        }
        if (e instanceof BookingStatusException) {
            return err(env, e.getMessage(), ErrorType.BAD_REQUEST, "INVALID_STATUS");
        }

        if (e instanceof ResourceNotFoundException) {
            return err(env, "Ресурс не найден", ErrorType.NOT_FOUND, "RESOURCE_NOT_FOUND");
        }
        if (e instanceof ResourceInactiveException) {
            return err(env, "Ресурс неактивен", ErrorType.BAD_REQUEST, "RESOURCE_INACTIVE");
        }
        if (e instanceof UserNotFoundException) {
            return err(env, "Пользователь не найден", ErrorType.NOT_FOUND, "USER_NOT_FOUND");
        }

        if (e instanceof DataIntegrityViolationException) {
            return err(env, "Операция нарушает ограничения данных (возможно, конфликт бронирования)",
                    ErrorType.BAD_REQUEST, "DATA_INTEGRITY_VIOLATION");
        }

        if (e instanceof NumberFormatException) {
            return err(env, "Некорректное значение amount. Пример: \"100.00\"",
                    ErrorType.BAD_REQUEST, "INVALID_AMOUNT");
        }
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() == null ? "Некорректные входные данные" : e.getMessage();

            if (msg.startsWith("No client for provider")) {
                return err(env, "Платёжный провайдер не поддерживается", ErrorType.BAD_REQUEST, "UNKNOWN_PROVIDER");
            }
            if (msg.contains("Invalid payloadJson")) {
                return err(env, "payloadJson должен быть валидным JSON", ErrorType.BAD_REQUEST, "INVALID_PAYLOAD");
            }
            return err(env, msg, ErrorType.BAD_REQUEST, "BAD_REQUEST");
        }

        if (ex instanceof PaymentAccessDeniedException) {
            return err(env,
                    "У вас недостаточно прав для выполнения этой операции",
                    ErrorType.FORBIDDEN,
                    "INSUFFICIENT_PERMISSIONS");
        }

        return null;
    }

    private GraphQLError err(DataFetchingEnvironment env, String message, ErrorType type, String code) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(type)
                .extensions(Map.of("code", code))
                .build();
    }

    private boolean isInvalidUuid(Throwable ex) {
        if (ex instanceof ConversionFailedException) return true;
        if (ex instanceof CoercingParseValueException) return true;

        Throwable c = ex;
        for (int i = 0; i < 15 && c != null; i++) {
            if (c instanceof CoercingParseValueException) return true;
            if (c instanceof IllegalArgumentException && c.getMessage() != null
                    && c.getMessage().toLowerCase().contains("uuid")) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }

    private Throwable unwrap(Throwable ex) {
        Throwable e = ex;

        while (e instanceof CompletionException && e.getCause() != null) {
            e = e.getCause();
        }

        Throwable c = e;
        int depth = 0;
        while (c.getCause() != null && depth < 10) {
            c = c.getCause();
            depth++;
        }

        return e;
    }
}
