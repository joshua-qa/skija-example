package com.shua.skijaexample.controller;

import com.shua.skijaexample.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @PostMapping("/resize")
    public Mono<Void> resize(@RequestPart("file") Mono<FilePart> file, ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.IMAGE_JPEG);
        return response.writeWith(mainService.convert(file));
    }

    @PostMapping("/cutGifFrame")
    public Mono<Void> cutGifFrame(@RequestPart("file") Mono<FilePart> file, @RequestParam("frame") int frame, ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.IMAGE_JPEG);
        return response.writeWith(mainService.cutGifFrame(file, frame));
    }
}
