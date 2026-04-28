package org.example.speechhelper.global.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GlobalController {

    @GetMapping("/interview/historyP")
    public String historyP(){return "history"; }

    @GetMapping("/interview/portfolioP")
    public String portfolioP(){ return "portfolio"; }

    @GetMapping("/index")
    public String indexP(){ return "index"; }
}
