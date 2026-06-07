package com.example.demo.mapper;

import com.example.demo.domain.Person;
import com.example.demo.dto.CreatePersonRequest;
import com.example.demo.dto.PersonResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Person toEntity(CreatePersonRequest request);

    PersonResponse toResponse(Person person);
}
