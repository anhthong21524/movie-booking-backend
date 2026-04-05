package com.moviebooking.common.response;

import java.util.List;

public class ProblemDetail {

    private String code;
    private String detail;
    private List<FieldError> errors;

    public ProblemDetail() {}

    public ProblemDetail(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public ProblemDetail(String code, String detail, List<FieldError> errors) {
        this.code = code;
        this.detail = detail;
        this.errors = errors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }
}
