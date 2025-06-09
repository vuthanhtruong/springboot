package com.example.demo.Repository;

import com.example.demo.ModelOOP.Persons;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PersonRepository extends JpaRepository<Persons, String> {
}