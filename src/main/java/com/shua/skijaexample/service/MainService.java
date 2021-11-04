package com.shua.skijaexample.service;

import com.shua.skijaexample.exception.ImageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.skija.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

    public Mono<DataBuffer> convert(Mono<FilePart> filePartMono) {
        return filePartMono.flatMap(filePart -> DataBufferUtils.join(filePart.content()))
                .publishOn(Schedulers.boundedElastic())
                .map(dataBuffer -> resizeAndFillColor(dataBuffer.asByteBuffer()))
                .onErrorMap(throwable -> new ImageProcessingException("이미지 처리 실패", throwable));
    }

    public Mono<DataBuffer> cutGifFrame(Mono<FilePart> filePartMono, int frame) {
        return filePartMono.flatMap(filePart -> DataBufferUtils.join(filePart.content()))
                .publishOn(Schedulers.boundedElastic())
                .map(dataBuffer -> cutGifFrameInternal(dataBuffer.asByteBuffer(), frame))
                .onErrorMap(throwable -> new ImageProcessingException("이미지 처리 실패", throwable));
    }

    /**
     * 리사이징하기
     * @param buffer image buffer (ByteBuffer)
     * @return DataBuffer result
     */
    private DataBuffer resizeImage(ByteBuffer buffer) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Image image = Image.makeFromEncoded(buffer.array());

        ImageInfo info = ImageInfo.makeN32Premul(460, 460);
        Surface surface = Surface.makeRasterN32Premul(460, 460);
        int rowBytes = info.getWidth() * 4;

        Data bufferData = Data.makeFromBytes(new byte[info.getHeight() * rowBytes]);
        ByteBuffer directBuffer = bufferData.toByteBuffer();
        Pixmap pixmap = Pixmap.make(info, directBuffer, rowBytes);
        image.scalePixels(pixmap, SamplingMode.LINEAR, false);

        surface.getCanvas().drawImage(Image.makeFromPixmap(pixmap), 0, 0);
        Data result = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 70);
        Assert.notNull(result, "Result is null");
        ByteBuffer jpgBytes = result.toByteBuffer();
        directBuffer.clear();
        buffer.clear();
        stopWatch.stop();
        log.info("time : {}", stopWatch.getTotalTimeMillis());
        return DefaultDataBufferFactory.sharedInstance.wrap(jpgBytes);
    }

    /**
     * 리사이징하고 남는 공간은 흰색으로 채우기
     * @param buffer image buffer (ByteBuffer)
     * @return DataBuffer result
     */
    private DataBuffer resizeAndFillColor(ByteBuffer buffer) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Image image = Image.makeFromEncoded(buffer.array());

        ImageInfo info = ImageInfo.makeN32Premul(460, 460);
        Surface surface = Surface.makeRasterN32Premul(640, 460);
        int rowBytes = info.getWidth() * 4;

        Data bufferData = Data.makeFromBytes(new byte[info.getHeight() * rowBytes]);
        ByteBuffer directBuffer = bufferData.toByteBuffer();
        Pixmap pixmap = Pixmap.make(info, directBuffer, rowBytes);
        image.scalePixels(pixmap, SamplingMode.LINEAR, false);

        Paint paint = new Paint();
        paint.setColor(0xffffffff);
        surface.getCanvas().drawPaint(paint).drawImage(Image.makeFromPixmap(pixmap), 90, 0);
        Data result = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 70);
        Assert.notNull(result, "Result is null");
        ByteBuffer jpgBytes = result.toByteBuffer();
        directBuffer.clear();
        buffer.clear();
        stopWatch.stop();
        log.info("time : {}", stopWatch.getTotalTimeMillis());
        return DefaultDataBufferFactory.sharedInstance.wrap(jpgBytes);
    }

    private DataBuffer cutGifFrameInternal(ByteBuffer buffer, int frame) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Data data = Data.makeFromBytes(buffer.array());
        Codec codec = Codec.makeFromData(data);

        Assert.isTrue(codec.getEncodedImageFormat().equals(EncodedImageFormat.GIF), "File format should be GIF");
        Bitmap bitmap = new Bitmap();
        bitmap.allocN32Pixels(codec.getWidth(), codec.getHeight());
        int frameCount = codec.getFrameCount();
        Assert.isTrue(frameCount >= frame, "invalid request param (frame)");
        codec.readPixels(bitmap, frame);

        Data result = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.JPEG, 70);
        Assert.notNull(result, "Result is null");
        ByteBuffer jpgBytes = result.toByteBuffer();
        stopWatch.stop();
        log.info("time : {}", stopWatch.getTotalTimeMillis());
        return DefaultDataBufferFactory.sharedInstance.wrap(jpgBytes);
    }

    private ByteBuffer convertImage(File file) {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (Exception e) {
            return ByteBuffer.allocate(0);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "JPEG", outputStream);
            return ByteBuffer.wrap(outputStream.toByteArray());
        } catch (Exception e) {
            return ByteBuffer.allocate(0);
        } finally {
            image.flush();
        }
    }
}
