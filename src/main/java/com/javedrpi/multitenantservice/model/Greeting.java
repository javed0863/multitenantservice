package com.javedrpi.multitenantservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "greetings")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

//    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Greeting() {}

    public Greeting(String message) {
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
