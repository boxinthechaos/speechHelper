package org.example.speechhelper.global.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
public class GlobalController {

    @GetMapping("/historyP")
    public String historyP(){return "history"; }

    @GetMapping("/portfolioP")
    public String portfolioP(){ return "portfolio"; }
}
