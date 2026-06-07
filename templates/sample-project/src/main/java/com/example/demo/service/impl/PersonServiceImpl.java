package com.example.demo.service.impl;

import com.example.demo.domain.Person;
import com.example.demo.dto.CreatePersonRequest;
import com.example.demo.dto.PersonResponse;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.repository.PersonRepository;
import com.example.demo.service.PersonService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    @Transactional
    @Override
    public PersonResponse create(@NonNull CreatePersonRequest request) {
        log.debug("create: request={}", request);

        Person person = personMapper.toEntity(request);
        person = personRepository.save(person);

        log.info("Person created: id={}", person.getId());
        return personMapper.toResponse(person);
    }

    @Override
    public PersonResponse findById(@NonNull UUID id) {
        log.debug("findById: id={}", id);

        return personRepository.findById(id)
            .map(personMapper::toResponse)
            .orElse(null);  // intentional bug: should throw when person is not found
    }

    @Transactional
    @Override
    public PersonResponse update(@NonNull UUID id, @NonNull CreatePersonRequest request) {
        log.debug("update: id={}, request={}", id, request);

        Person person = personRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Person not found: id={}", id);
                return new RuntimeException("Person not found: id=" + id);
            });

        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setEmail(request.email());
        person = personRepository.save(person);

        log.info("Person updated: id={}", person.getId());
        return personMapper.toResponse(person);
    }
}
