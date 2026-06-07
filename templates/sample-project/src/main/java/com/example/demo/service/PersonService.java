package com.example.demo.service;

import com.example.demo.dto.CreatePersonRequest;
import com.example.demo.dto.PersonResponse;

import lombok.NonNull;

import java.util.UUID;

public interface PersonService {

    PersonResponse create(@NonNull CreatePersonRequest request);

    PersonResponse findById(@NonNull UUID id);

    PersonResponse update(@NonNull UUID id, @NonNull CreatePersonRequest request);
}
