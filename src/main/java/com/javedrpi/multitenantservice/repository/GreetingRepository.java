package com.javedrpi.multitenantservice.repository;

import com.javedrpi.multitenantservice.model.Greeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreetingRepository extends JpaRepository<Greeting, Long> {}
