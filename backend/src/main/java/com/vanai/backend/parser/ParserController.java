package com.vanai.backend.parser;

import com.vanai.backend.parser.dto.ParserRequest;
import com.vanai.backend.parser.dto.ParserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parser")
@CrossOrigin(origins = "http://localhost:5173")
public class ParserController {

    private final ParserService parserService;

    public ParserController(ParserService parserService) {
        this.parserService = parserService;
    }

    @PostMapping
    public ParserResponse parse(@Valid @RequestBody ParserRequest request) {
        return parserService.parse(request.url());
    }
}
