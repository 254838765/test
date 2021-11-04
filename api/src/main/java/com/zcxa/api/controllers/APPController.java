package com.zcxa.api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class APPController {


    @GetMapping("sayHello/{name}/{age}")
    public String sayHello(@PathVariable String name,@PathVariable String age){
        return "SUCCESS";
    }

}
