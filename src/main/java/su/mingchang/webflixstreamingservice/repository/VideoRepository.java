package su.mingchang.webflixstreamingservice.repository;

import su.mingchang.webflixstreamingservice.model.Video;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VideoRepository {
    Mono<Video> getVideoByName(String name);
    Flux<Video> getAllVideos();
    Mono<Video> addVideo(Video video);
}
