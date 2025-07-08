package com.ob.api.dx.util;

import com.ob.api.dx.model.response.ErrorResponse;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.client.rest.HttpCode;
import com.ob.broker.common.client.rest.ResponseMessage;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatusCode;

@UtilityClass
public class ErrorUtil {
    public static void throwIfError(ResponseMessage responseMessage) {
        if (HttpStatusCode.valueOf(responseMessage.code()).is4xxClientError()) {
            String description = "";
            try {
                ErrorResponse errorResponse = JsonService.JSON.fromJson(responseMessage.message(), ErrorResponse.class);
                description = errorResponse.getDescription();
            } catch (Exception e) {

            }
            if (responseMessage.code() == HttpCode.UNAUTHORIZED.getCode()) {
                throw new CodeException(description, Code.UNAUTHORIZED);
            } else if (responseMessage.code() == HttpCode.METHOD_NOT_ALLOWED.getCode()) {
                throw new CodeException(description, Code.METHOD_NOT_SUPPORTED);
            } else if (responseMessage.code() == HttpCode.NOT_FOUND.getCode()) {
                throw new CodeException(description, Code.NOT_FOUND);
            } else if (responseMessage.code() == HttpCode.TOO_MANY_REQUESTS.getCode()) {
                throw new CodeException(description, Code.TOO_FREQUENT_REQUEST);
            } else if (responseMessage.code() == HttpCode.BAD_REQUEST.getCode()) {
                throw new CodeException(description, Code.BAD_REQUEST);
            } else {
                throw new CodeException(description, Code.INVALID_REQUEST);
            }
        }
    }

}
