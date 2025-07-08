package com.ob.broker.common.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties({"cause", "stackTrace", "localizedMessage", "suppressed"})
public class CodeException extends RuntimeException {
    private final int code;

    public CodeException(String message, int code) {
        super(message);
        this.code = code;
    }

    public CodeException(String message, Throwable e, Code code) {
        super(message, e);
        this.code = code.getValue();
    }

    public CodeException(String message, Throwable e, int code) {
        super(message, e);
        this.code = code;
    }

    public CodeException(Throwable e, int code) {
        super(e);
        this.code = code;
    }

    public CodeException(String message, Code code) {
        super(message);
        this.code = code.getValue();
    }

    public CodeException(Throwable e, Code code) {
        super(e.getMessage() == null ? code.name() : e.getMessage(), e);
        this.code = code.getValue();
    }

    public CodeException(Code code) {
        super(code.name());
        this.code = code.getValue();
    }

    public CodeException(Throwable e) {
        super(e);
        if (e instanceof CodeException)
            this.code = ((CodeException) e).code;
        else
            this.code = -1;
    }

    public static CodeException addMessage(CodeException codeException, String message) {
        String newMessage = codeException.getMessage() + " | " + message;
        return new CodeException(newMessage, codeException, codeException.code);
    }

    @Override
    public String toString() {
        return "CodeException:" + getMessage();
    }

    @Override
    public String getMessage() {
        return Code.getById(code) + "|" + super.getMessage();
    }
}
