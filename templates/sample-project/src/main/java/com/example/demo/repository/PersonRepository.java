package com.example.demo.repository;

import com.example.demo.domain.Person;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends CrudRepository<Person, UUID> {

    Optional<Person> findByEmail(String email);
}
