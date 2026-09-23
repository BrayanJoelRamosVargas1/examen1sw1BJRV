package com.umlcase.application.mapper.relational;

import org.springframework.stereotype.Component;

@Component
public class SqlNamingStrategy {

    public String toSnakeCase(String str) {
        if (str == null) return "";
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
