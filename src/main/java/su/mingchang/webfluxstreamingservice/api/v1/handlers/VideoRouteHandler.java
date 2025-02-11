package su.mingchang.webfluxstreamingservice.api.v1.handlers;

import lombok.extern.slf4j.Slf4j;
import su.mingchang.webfluxstreamingservice.model.Video;
import su.mingchang.webfluxstreamingservice.repository.VideoRepository;
import su.mingchang.webfluxstreamingservice.services.IFileService;
import su.mingchang.webfluxstreamingservice.services.VideoService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class VideoRouteHandler {

    private final VideoService videoService;
    private final IFileService fileService;
    private final VideoRepository videoRepository;

    @Autowired
    public VideoRouteHandler(VideoService videoService, IFileService fileService, VideoRepository videoRepository) {
        this.videoService = videoService;
        this.fileService = fileService;
        this.videoRepository = videoRepository;
    }


    public Mono<ServerResponse> listVideos(ServerRequest request) {

        fileService.getAllFiles()
                .doOnNext(path -> log.debug("found file in path: " + path.toUri() + " FileName: " + path.getFileName()))
                .flatMap(path -> {
                    Video video = new Video();
                    video.setName(path.getFileName().toString());
                    video.setLocation(path);
                    return videoRepository.addVideo(video);
                })
                .subscribe();
        Flux<Video> videos = videoService.getAllVideos();

        Flux<VideoDetails> videoDetailsFlux = videos
                .map(video -> {
                    VideoDetails videoDetails = new VideoDetails();
                    videoDetails.setName(video.getName());
                    videoDetails.setLink(request.uri().toString() + '/' + video.getName().substring(0, video.getName().length()-4));
                    return videoDetails;
                })
                .doOnError(t -> {
                    throw Exceptions.propagate(t);
                });

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache())
                .location(request.uri())
                .body(videoDetailsFlux, VideoDetails.class);
    }

    public Mono<ServerResponse> getPartialContent(ServerRequest request) {
        String name = request.pathVariable("name") + ".mp4";
        Mono<ResourceRegion> resourceRegionMono = videoService.getRegion(name, request);

        return resourceRegionMono
                .flatMap(resourceRegion -> ServerResponse
                        .status(HttpStatus.PARTIAL_CONTENT)
                        .contentLength(resourceRegion.getCount())
                        .headers(headers -> headers.setCacheControl(CacheControl.noCache()))
                        .body(resourceRegionMono, ResourceRegion.class))
                .doOnError(throwable -> {
                    throw Exceptions.propagate(throwable);
                });
    }

    /**
     * This function gets a file from the file system and returns it as a whole
     * videoResource.contentLength() is a blocking call, therefore it is wrapped in a Mono.
     * it returns a FileNotFound exception which is wrapped and propagated down the stream
     */
    public Mono<ServerResponse> getFullContent(ServerRequest request) {

        String fileName = request.pathVariable("name") + ".mp4";

        Mono<UrlResource> videoResourceMono = videoService.getResourceByName(fileName);


        return videoResourceMono
                .flatMap(urlResource -> {
                    long contentLength = videoService.lengthOf(urlResource);
                    return ServerResponse
                            .ok()
                            .contentLength(contentLength)
                            .headers(httpHeaders -> httpHeaders.setCacheControl(CacheControl.noCache()))
                            .body(videoResourceMono, UrlResource.class);
                });

    }

    @Data
    private static class VideoDetails {
        private String name;
        private String link;
    }
}
