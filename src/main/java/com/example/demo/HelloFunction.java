package com.example.demo;

import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class HelloFunction implements Function<String, String> {
    @Override
    public String apply(String input) {
        return "Hello, " + (input != null ? input : "Vercel") + "!";
    }
}