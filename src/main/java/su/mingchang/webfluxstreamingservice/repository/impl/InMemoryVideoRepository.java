package su.mingchang.webfluxstreamingservice.repository.impl;

import su.mingchang.webfluxstreamingservice.exceptions.VideoNotFoundException;
import su.mingchang.webfluxstreamingservice.model.Video;
import su.mingchang.webfluxstreamingservice.repository.VideoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryVideoRepository implements VideoRepository {

    private final Map<String, Video> videoCache = new ConcurrentHashMap<>();

    @Override
    public Mono<Video> getVideoByName(String name) {
        return Mono.create(videoMonoSink -> {
            Video video = videoCache.get(name);
            if (video != null)
                videoMonoSink.success(video);
            else
                videoMonoSink.error(new VideoNotFoundException());
        });
    }

    @Override
    public Flux<Video> getAllVideos() {
        synchronized (videoCache) {
            return Flux.fromIterable(videoCache.values());
        }
    }

    @Override
    public Mono<Video> addVideo(Video video) {
        synchronized (videoCache) {
            return Mono.fromCallable(() -> videoCache.put(video.getName(), video));
        }
    }
}
