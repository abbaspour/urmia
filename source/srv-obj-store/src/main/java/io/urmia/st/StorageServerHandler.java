package io.urmia.st;

/**
 *
 * Copyright 2014 by Amin Abbaspour
 *
 * This file is part of Urmia.io
 *
 * Urmia.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Urmia.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.urmia.util.DigestUtils;
import io.urmia.util.FileTime;
import io.urmia.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class StorageServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(StorageServerHandler.class);

    private final String BASE;

    private HttpRequest request;

    boolean readingChunks = false;

    FileChannel fileChannel = null;

    public StorageServerHandler(String BASE) {
        this.BASE = BASE;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.read();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught", cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {


        if (msg instanceof HttpRequest) {

            request = (HttpRequest) msg;

            log.info("received HttpRequest: {} {}", request.getMethod(), request.getUri());

            // todo: what's this?
            //if (is100ContinueExpected(request)) {
            //    send100Continue(ctx);
            //}

            if (HttpMethod.PUT.equals(request.getMethod())) {
                // start doing the fs put. next msg is not a LastHttpContent
                handlePUT(ctx, request);
                //return;
            }

            ctx.read();
            return;
        }

        if (msg instanceof HttpContent) {

            // New chunk is received
            HttpContent chunk = (HttpContent) msg;


            //log.info("chunk {} of size: {}", chunk, chunk.content().readableBytes());

            if (fileChannel != null) {
                writeToFile(chunk.content());
            }

            // example of reading only if at the end
            if (chunk instanceof LastHttpContent) {

                log.trace("received LastHttpContent: {}", chunk);

                if (HttpMethod.HEAD.equals(request.getMethod())) {
                    handleHEAD(ctx, request);
                    ctx.read();
                    return;
                }

                if (HttpMethod.GET.equals(request.getMethod())) {
                    handleGET(ctx, request);
                    ctx.read();
                    return;
                }

                if (HttpMethod.DELETE.equals(request.getMethod())) {
                    handleDELETE(ctx, request);
                    ctx.read();
                    return;
                }


                if (HttpMethod.PUT.equals(request.getMethod())) {
                    // TODO: reset() if exception catch or timeout (i.e. no LastHttpContent)
                    writeResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK), true); // close the connection after upload (mput) done
                    reset();
                    ctx.read();
                    return;
                }

                log.warn("unknown request: {}", request);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            }

            ctx.read();
            return;
        }

        log.warn("unknown msg type: {}", msg);
    }

    private void handleGET(ChannelHandlerContext ctx, HttpRequest request) throws IOException {

        log.info("handleGET req: {}", request);

        final String uri = request.getUri();

        // todo: handle args: limit, object=true, directory=true, marker=xyz, sort_order=reverse, sort=mtime
        final int queryPos = uri.lastIndexOf('?');
        final String uriNoArgs = queryPos == -1 ? uri : uri.substring(0, queryPos);

        log.info("GET uriNoArgs: {}", uriNoArgs);

        final String path = BASE + uriNoArgs;

        File file = new File(path);

        if (file.exists()) {
            if (file.isFile()) {
                downloadFile(ctx, file);
                return;
            }

            if (file.isDirectory()) {
                listDirectory(ctx, file);
                return;
            }
        }

        sendError(ctx, NOT_FOUND);
    }

    private ByteBuf errorBody(String code, String message) {
        Map<String, Object> m = new HashMap<String, Object>(2);
        m.put("code", code);
        m.put("message", message);

        String json = StringUtils.mapToJson(m);
        return Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
    }

    private void handleDELETE(ChannelHandlerContext ctx, HttpRequest request) throws IOException {
        log.info("handleDELETE req: {}", request);

        final String uri = request.getUri();

        final int queryPos = uri.lastIndexOf('?');
        final String uriNoArgs = queryPos == -1 ? uri : uri.substring(0, queryPos);

        log.info("DELETE uriNoArgs: {}", uriNoArgs);

        final String path = BASE + uriNoArgs;

        File file = new File(path);

        if (!file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }


        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length != 0) {
                writeResponse(ctx,
                        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                                errorBody("DirectoryNotEmptyError", uriNoArgs + " is not empty")), true);
                //sendError(ctx, HttpResponseStatus.BAD_REQUEST); // dir not empty
                return;
            }
        }

        if (file.delete()) {
            log.info("deleted: {}", path);
            writeResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT), true);
        } else {
            log.info("unable to delete: {}", path);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        }

    }

    private static final Joiner LINE_JOINER = Joiner.on('\n').skipNulls();

    private void listDirectory(ChannelHandlerContext ctx, File file) throws IOException {

        log.info("listDirectory: {}", file);

        File[] files = file.listFiles();

        if (files == null) {
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            return;
        }

        List<String> jsons = new ArrayList<String>(files.length);

        for (File f : files) {
            log.debug("found: {}", f);
            jsons.add(toJson(f));
        }

        String result = LINE_JOINER.join(jsons);

        log.info("writing back: {}", result);

        HttpResponseStatus status = HttpResponseStatus.OK;

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(result, CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/x-json-stream; type=directory");
        response.headers().set(CONTENT_LENGTH, result.length());
        response.headers().set("result-set-size", files.length);


        // Write the end marker
        ChannelFuture lastContentFuture = ctx.writeAndFlush(response);

        // Decide whether to close the connection or not.
        if (!isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String toJson(File f) throws IOException {

        Map<String, Object> map = new HashMap<String, Object>(6);
        //Path path = f.toPath();

        map.put("name", f.getName());
        //TODO map.put("etag", calcETAG(f));
        map.put("size", f.length());
        map.put("type", f.isDirectory() ? "directory" : "object");
        map.put("mtime", FileTime.fromMillis(f.lastModified()));
        map.put("durability", 1);

        return StringUtils.mapToJson(map);
    }

    private void downloadFile(ChannelHandlerContext ctx, File file) throws IOException {

        final RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            log.warn("no file at: {}", file.getPath());
            sendError(ctx, NOT_FOUND);
            return;
        }
        final long fileLength = raf.length();

        log.info("downloading file: {} of len: {}", file, fileLength);


        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        //setDateAndCacheHeaders(response, file);

        if (isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        final ChannelFuture sendFileFuture;

        sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());


        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println("Transfer progress: " + progress);
                } else {
                    System.err.println("Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.err.println("Transfer complete.");
            }
        });

        // Write the end marker
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        // Decide whether to close the connection or not.
        if (!isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     * @param file     file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private void writeToFile(ByteBuf buf) throws IOException {
        if (fileChannel == null)
            return;
        fileChannel.write(buf.nioBuffers());
    }

    private void reset() throws IOException {
        request = null;
        readingChunks = false;
        if (fileChannel != null) {
            fileChannel.close();
            fileChannel = null;
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, FullHttpResponse response, boolean forceClose) {

        // Decide whether to close the connection or not.
        boolean close = forceClose
                || HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.headers().get(HttpHeaders.Names.CONNECTION))
                || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
                && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.headers().get(HttpHeaders.Names.CONNECTION));

        log.debug("writeResponse close: {}, data: {}", close, response);

        ChannelFuture future = ctx.writeAndFlush(response);
        // Close the connection after the write operation is done if necessary.
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleHEAD(ChannelHandlerContext ctx, HttpRequest request) throws IOException {

        log.info("handleHEAD uri: {} ", request.getUri());

        HttpResponseStatus status = HttpResponseStatus.NO_CONTENT;

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);

        String uri = request.getUri(); // HEAD /abbaspour/stor/20120624_002h.jpg HTTP/1.1

        final String path = BASE + uri;

        File f = new File(path);

        if (f.exists()) {
            if (f.isDirectory()) {
                response.headers().set(CONTENT_TYPE, "application/x-json-stream; type=directory");
            } else {
                setContentTypeHeader(response, f);
                response.headers().set(CONTENT_MD5, DigestUtils.md5sum(f));
            }
        }

        ctx.writeAndFlush(response); // VoidChannelPromise
    }

    private static boolean isDirectory(String contentType) {
        return contentType != null && contentType.endsWith("; type=directory");
    }

    private void handlePUT(ChannelHandlerContext ctx, HttpRequest request) throws IOException {

        log.debug("handlePUT: {}", request);

        final String location = getLocation(request);

        String uri = request.getUri();

        if (location != null) {
            String existing = BASE + location;
            String link = BASE + uri;
            log.debug("ln {} -> {}", link, existing);
            link(ctx, existing, link);
            return;
        }

        boolean isDir = isDirectory(request.headers().get("content-type"));
        // PUT

        String p = BASE + uri;

        log.info("creating {} at: {}", isDir ? "dir" : "file", p);

        File file = new File(p);

        if (isDir) {
            log.info("mkdir at: {}", p);
            final boolean done = file.mkdirs();

            if (done)
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT));
            else
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);

        } else {
            file.getParentFile().mkdirs();

            // todo: handle 'location' header for 'mln()'. e.i. no content
            //Path path = file.toPath();
            //fileChannel = FileChannel.open(path, CREATE, WRITE);
            fileChannel = new FileOutputStream(file).getChannel(); // todo: should close fis?

            readingChunks = HttpHeaders.isTransferEncodingChunked(request);
            //log.info("is chunk: {}", readingChunks);

            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)); // VoidChannelPromise
        }
    }

    String getLocation(HttpRequest request) {
        return request.headers().get("location");
    }

    void link(ChannelHandlerContext ctx, String existing, String link) {
        File existingFile = new File(existing);

        if (!existingFile.exists()) {
            log.warn("does not exist: {}", existing);
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        File destFile = new File(link);
        if(destFile.exists()) {
            log.warn("destination already exist: {}", link);
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        try {
            copyFile(existingFile, destFile);
        } catch (IOException e) {
            log.warn("error on link {} -> {}. error: {}", link, existing, e.getMessage());
            //sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            writeResponse(ctx,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                            errorBody("LinkError", e.getClass().getSimpleName())), true);

        }

        writeResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT), false);
    }

    private static void copyFile(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null)
                try {
                    inputChannel.close();
                } catch (Exception ignored) {
                }

            if (outputChannel != null)
                try {
                    outputChannel.close();
                } catch (Exception ignored) {
                }
        }
    }
}