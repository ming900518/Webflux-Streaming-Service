package su.mingchang.webfluxstreamingservice.services.impl;

import su.mingchang.webfluxstreamingservice.services.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class IFileServiceImpl implements IFileService {

    @Value("${video.location}")
    private String videoLocation;

    @Override
    public Flux<Path> getAllFiles() {
        return fromPath(Paths.get(videoLocation));
    }
}
