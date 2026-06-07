package com.example.demo;

import com.example.demo.domain.Person;
import com.example.demo.dto.CreatePersonRequest;
import com.example.demo.dto.PersonResponse;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.repository.PersonRepository;
import com.example.demo.service.impl.PersonServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private PersonServiceImpl personService;

    @Test
    void create_whenValidRequest_savesAndReturnsResponse() {
        // given
        var request = new CreatePersonRequest("John", "Doe", "john@example.com");
        var entity = new Person();
        entity.setId(UUID.randomUUID());
        var expected = new PersonResponse(entity.getId(), "John", "Doe", "john@example.com", Instant.now());

        when(personMapper.toEntity(request)).thenReturn(entity);
        when(personRepository.save(entity)).thenReturn(entity);
        when(personMapper.toResponse(entity)).thenReturn(expected);

        // when
        var result = personService.create(request);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findById_whenPersonExists_returnsResponse() {
        // given
        var id = UUID.randomUUID();
        var entity = new Person();
        entity.setId(id);
        var expected = new PersonResponse(id, "Jane", "Doe", "jane@example.com", Instant.now());

        when(personRepository.findById(id)).thenReturn(Optional.of(entity));
        when(personMapper.toResponse(entity)).thenReturn(expected);

        // when
        var result = personService.findById(id);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findById_whenPersonDoesNotExist_throwsException() {
        // given
        var id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> personService.findById(id))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Person not found");
    }
}
