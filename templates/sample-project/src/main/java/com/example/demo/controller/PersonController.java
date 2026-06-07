package com.example.demo.controller;

import com.example.demo.dto.CreatePersonRequest;
import com.example.demo.dto.PersonResponse;
import com.example.demo.service.PersonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/persons")
@Tag(name = "Person", description = "Person management operations")
public class PersonController {

    private final PersonService personService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new person")
    public PersonResponse create(@RequestBody @Valid CreatePersonRequest request) {
        return personService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find person by ID")
    public PersonResponse findById(@PathVariable UUID id) {
        return personService.findById(id);
    }
}
