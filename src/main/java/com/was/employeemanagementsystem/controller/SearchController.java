package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/search")
@CrossOrigin(origins = "${app.cors.origins}")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Global search across employees, documents, rota, and leaves
     * Only ADMIN and SUPER_ADMIN can use this endpoint
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SearchService.SearchResults> search(@RequestParam("q") String query) {
        log.info("🔍 Global search request: '{}'", query);
        SearchService.SearchResults results = searchService.search(query);
        return ResponseEntity.ok(results);
    }
}

