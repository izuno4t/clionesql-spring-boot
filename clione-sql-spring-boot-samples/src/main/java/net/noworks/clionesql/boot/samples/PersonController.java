package net.noworks.clionesql.boot.samples;

import java.util.List;

import tetz42.clione.util.ResultMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for person CRUD operations.
 *
 * <p>
 * Provides endpoints for listing, searching, retrieving, creating, and updating persons.
 */
@RestController
@RequestMapping("/persons")
public class PersonController {

    private final PersonService personService;

    /**
     * Creates a new {@code PersonController}.
     *
     * @param personService
     *            the person service
     */
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * Retrieves all persons.
     *
     * @return a list of all person records
     */
    @GetMapping
    public List<ResultMap> findAll() {
        return personService.findAll();
    }

    /**
     * Searches for persons by name.
     *
     * @param name
     *            the name to search for (must not be blank)
     *
     * @return a list of matching person records
     *
     * @throws ResponseStatusException
     *             400 Bad Request if name is blank
     */
    @GetMapping("/search")
    public List<ResultMap> findByName(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        return personService.findByName(name);
    }

    /**
     * Retrieves a single person by ID.
     *
     * @param id
     *            the person ID
     *
     * @return the matching person record
     */
    @GetMapping("/{id}")
    public ResultMap findById(@PathVariable int id) {
        return personService.findById(id);
    }

    /**
     * Creates a new person.
     *
     * @param id
     *            the person ID
     * @param name
     *            the person's name
     * @param status
     *            the person's status
     *
     * @return the number of rows affected
     */
    @PostMapping
    public int insert(@RequestParam int id, @RequestParam String name, @RequestParam String status) {
        return personService.insert(id, name, status);
    }

    /**
     * Updates the status of an existing person.
     *
     * @param id
     *            the person ID
     * @param status
     *            the new status value
     *
     * @return the number of rows affected
     */
    @PutMapping("/{id}/status")
    public int updateStatus(@PathVariable int id, @RequestParam String status) {
        return personService.updateStatus(id, status);
    }
}
