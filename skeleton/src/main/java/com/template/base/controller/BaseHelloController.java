package com.template.base.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseHelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello from Kodly base skeleton";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
