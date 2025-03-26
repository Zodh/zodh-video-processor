package io.github.zodh.processor.adapter.controller;

import io.github.zodh.processor.core.application.usecases.ProcessorVideoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
public class ProcessorController {

    private final ProcessorVideoUseCase processorVideoUseCase;

    public ProcessorController(ProcessorVideoUseCase processorVideoUseCase) {
        this.processorVideoUseCase = processorVideoUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> processVideo(@RequestParam("file") MultipartFile file) {
        try {
            processorVideoUseCase.execute(file);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}