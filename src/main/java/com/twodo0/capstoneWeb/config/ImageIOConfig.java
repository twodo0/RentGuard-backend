package com.twodo0.capstoneWeb.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Configuration
public class ImageIOConfig {

    @PostConstruct
    public void initImageIO(){
        ImageIO.setUseCache(false);

        ImageIO.scanForPlugins();

        List<String> readers = new ArrayList<>();
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("jpeg");
        while (it.hasNext()) readers.add(it.next().getClass().getName());
        log.info("[ImageIO] JPEG readers = {}", readers);

    }
}
