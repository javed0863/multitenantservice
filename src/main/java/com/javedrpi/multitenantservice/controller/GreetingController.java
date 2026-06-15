package com.javedrpi.multitenantservice.controller;

import com.javedrpi.multitenantservice.repository.GreetingRepository;
import com.javedrpi.multitenantservice.model.Greeting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

    private final GreetingRepository greetingRepository;

    public GreetingController(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    @PostMapping
    public Greeting save(@RequestBody String message) {
        return greetingRepository.save(new Greeting(message));
    }

    @GetMapping
    public List<Greeting> findAll() {
        return greetingRepository.findAll();
    }
}
