package net.juancarlosfernandez.jhipster.web.rest;

import com.codahale.metrics.annotation.Timed;
import net.juancarlosfernandez.jhipster.domain.Weight;

import net.juancarlosfernandez.jhipster.repository.UserRepository;
import net.juancarlosfernandez.jhipster.repository.WeightRepository;
import net.juancarlosfernandez.jhipster.repository.search.WeightSearchRepository;
import net.juancarlosfernandez.jhipster.security.AuthoritiesConstants;
import net.juancarlosfernandez.jhipster.security.SecurityUtils;
import net.juancarlosfernandez.jhipster.web.rest.util.HeaderUtil;
import net.juancarlosfernandez.jhipster.web.rest.util.PaginationUtil;
import net.juancarlosfernandez.jhipster.web.rest.vm.WeightByPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Weight.
 */
@RestController
@RequestMapping("/api")
public class WeightResource {

    private final Logger log = LoggerFactory.getLogger(WeightResource.class);

    @Inject
    private WeightRepository weightRepository;

    @Inject
    private WeightSearchRepository weightSearchRepository;

    @Inject
    private UserRepository userRepository;

    /**
     * POST  /weights : Create a new weight.
     *
     * @param weight the weight to create
     * @return the ResponseEntity with status 201 (Created) and with body the new weight, or with status 400 (Bad Request) if the weight has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/weights")
    @Timed
    public ResponseEntity<Weight> createWeight(@Valid @RequestBody Weight weight) throws URISyntaxException {
        log.debug("REST request to save Weight : {}", weight);
        if (weight.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("weight", "idexists", "A new weight cannot already have an ID")).body(null);
        }

        // Add user credentials when the user is not admin.
        if(!SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)){
            log.debug("No user passed in, using current user: {} ", SecurityUtils.getCurrentUserLogin());
            weight.setUser(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get());
        }

        Weight result = weightRepository.save(weight);
        weightSearchRepository.save(result);
        return ResponseEntity.created(new URI("/api/weights/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("weight", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /weights : Updates an existing weight.
     *
     * @param weight the weight to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated weight,
     * or with status 400 (Bad Request) if the weight is not valid,
     * or with status 500 (Internal Server Error) if the weight couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/weights")
    @Timed
    public ResponseEntity<Weight> updateWeight(@Valid @RequestBody Weight weight) throws URISyntaxException {
        log.debug("REST request to update Weight : {}", weight);
        if (weight.getId() == null) {
            return createWeight(weight);
        }
        Weight result = weightRepository.save(weight);
        weightSearchRepository.save(result);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("weight", weight.getId().toString()))
            .body(result);
    }

    /**
     * GET  /weights : get all the weights.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of weights in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/weights")
    @Timed
    public ResponseEntity<List<Weight>> getAllWeights(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Weights");
        Page<Weight> page;
        // Only admin user can see all the information. Normal users can only see his data.
        if(SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN))
            page = weightRepository.findAllByOrderByDateTimeDesc(pageable);
        else
            page = weightRepository.findByUserIsCurrentUser(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/weights");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /weights/:id : get the "id" weight.
     *
     * @param id the id of the weight to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the weight, or with status 404 (Not Found)
     */
    @GetMapping("/weights/{id}")
    @Timed
    public ResponseEntity<Weight> getWeight(@PathVariable Long id) {
        log.debug("REST request to get Weight : {}", id);
        Weight weight = weightRepository.findOne(id);
        return Optional.ofNullable(weight)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /weights/:id : delete the "id" weight.
     *
     * @param id the id of the weight to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/weights/{id}")
    @Timed
    public ResponseEntity<Void> deleteWeight(@PathVariable Long id) {
        log.debug("REST request to delete Weight : {}", id);
        weightRepository.delete(id);
        weightSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("weight", id.toString())).build();
    }

    /**
     * SEARCH  /_search/weights?query=:query : search for the weight corresponding
     * to the query.
     *
     * @param query the query of the weight search
     * @param pageable the pagination information
     * @return the result of the search
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/_search/weights")
    @Timed
    public ResponseEntity<List<Weight>> searchWeights(@RequestParam String query, Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to search for a page of Weights for query {}", query);
        Page<Weight> page = weightSearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/weights");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET /weight-by-days : get all weight readings for the last x days
     */
    @GetMapping("/weights-by-days/{days}")
    @Timed
    public ResponseEntity<WeightByPeriod> getByDays(@PathVariable int days){
        ZonedDateTime rightNow = ZonedDateTime.now();
        ZonedDateTime daysAgo = rightNow.minusDays(days);

        List<Weight> readings = weightRepository.findAllByDateTimeBetweenAndUserLoginOrderByDateTimeDesc(daysAgo,rightNow, SecurityUtils.getCurrentUserLogin());
        WeightByPeriod result = new WeightByPeriod("Last " + days + " Days",readings);
        return new ResponseEntity<WeightByPeriod>(result,HttpStatus.OK);
    }
}
