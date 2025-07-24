/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.videotools.streamingapi.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xyz
 */
public class StreamGetter {

    private final Logger logger;
    private final String m3u8Url;
    private final String baseUrl;
    private ArrayList<String> playlist;
    private long firstSeqNo;

    public StreamGetter(String m3u8Url) {

        logger = LoggerFactory.getLogger(getClass());
        this.m3u8Url = m3u8Url;
        baseUrl = m3u8Url.substring(0, m3u8Url.lastIndexOf('/') + 1);
        playlist = new ArrayList<>();
        firstSeqNo = -1;
    }

    // TODO: for more reliable use outside this mini-project:
    // have this in the constructor, so that it's clear that it must be called first
    public void loadPlaylist()
            throws URISyntaxException, MalformedURLException, IOException, FileNotFoundException {

        playlist.clear();
        URL url = new URI(m3u8Url).toURL();
        // can throw a MalformedURLException
        // (i.e. the inputName possibly has a typo)
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        // can throw an IOException
        // (i.e. probably a network error)
        int httpCode = httpCon.getResponseCode();
        if ((httpCode == 200) || (httpCode == 206) || (httpCode == 304)) {
            // resource is okay, so proceed
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpCon.getInputStream()))) {
                // comment: BufferedReader isn't necessary, since it's in-memory copying
                String line = null;
                while ((line = reader.readLine()) != null) {
                    playlist.add(line);
                    // System.out.println(line);
                }
            }
        } else {
            // some server error, possibly 404 (not found)
            throw new FileNotFoundException("URL '" + m3u8Url
                    + "' connection with HTTP error code " + httpCode);
        }
    }

    public byte[] loadStreamData(String streamUrl)
            throws URISyntaxException, MalformedURLException, IOException, FileNotFoundException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8 * 1024 * 1024); // 8 MiB should be enough

        URL url = new URI(streamUrl).toURL();
        // can throw a MalformedURLException
        // (i.e. the inputName possibly has a typo)
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        // can throw an IOException
        // (i.e. probably a network error)
        int httpCode = httpConn.getResponseCode();
        if (httpCode == HttpURLConnection.HTTP_OK) {
            // logger.debug("Content type: {}", httpConn.getContentType());
            // logger.debug("Content length: {}", httpConn.getContentLength());

            // resource is okay, so proceed
            InputStream inputStream = httpConn.getInputStream();

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        } else {
            // some server error, possibly 404 (not found)
            throw new FileNotFoundException("URL '" + streamUrl
                    + "' connection with HTTP error code " + httpCode);
            // but code 206 Partial Content also isn't accepted
        }

        return outputStream.toByteArray();
    }

    public long getFirstSequenceNo() {

        firstSeqNo = -1; // returning -1 means error (or: no entry found)

        ListIterator<String> playlistIterator = playlist.listIterator();
        while ((playlistIterator.hasNext()) && (firstSeqNo == -1)) {
            String line = playlistIterator.next();
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")) {
                String numberAsString = line.substring(line.lastIndexOf(':') + 1);
                firstSeqNo = Long.parseUnsignedLong(numberAsString);
                // System.out.println("Retrieved string '" + numberAsString + "', as number: " +
                // firstSeqNo);
            }
        }

        return firstSeqNo;
    }

    public Duration getDurationOfSequenceNo(long seqNo) {

        if (firstSeqNo == -1) {
            // should not happen, since getFirstSequenceNo()
            // should have been called already
            // but never-the-less:
            getFirstSequenceNo(); // to have it set
        }

        Duration duration = Duration.ZERO;

        // scan for the right url
        if (firstSeqNo != -1) {
            long currentSeqNo = firstSeqNo - 1;
            ListIterator<String> playlistIterator = playlist.listIterator();
            while ((playlistIterator.hasNext()) && (duration.isZero())) {
                String line = playlistIterator.next();
                if (line.startsWith("#EXTINF")) {
                    currentSeqNo++;
                    if (currentSeqNo == seqNo) {
                        // expected format:
                        // #EXTINF:6.000,
                        String durAsString = line
                                .substring(line.lastIndexOf(':') + 1, line.length() - 1);
                        duration = Duration
                                .ofMillis((long) (Double.parseDouble(durAsString) * 1000.0));
                        // logger.debug("#EXTINF line is '{}', extracted time info is '{}', as double
                        // that's {}, the duration obj. is {}.",
                        // line, durAsString, Double.parseDouble(durAsString), duration);
                    }
                }
            }
        }

        return duration;
    }

    public byte[] loadSequenceNo(long seqNo) {

        if (firstSeqNo == -1) {
            // should not happen, since getFirstSequenceNo()
            // should have been called already
            // but never-the-less:
            getFirstSequenceNo(); // to have it set
        }

        byte[] streamData = null;

        // scan for the right url
        if (firstSeqNo != -1) {
            String streamUrl = null;
            long currentSeqNo = firstSeqNo - 1;
            ListIterator<String> playlistIterator = playlist.listIterator();
            while ((playlistIterator.hasNext()) && (streamData == null)) {
                String line = playlistIterator.next();
                if (line.startsWith("#EXTINF")) {
                    currentSeqNo++;
                    if (currentSeqNo == seqNo) {
                        if (playlistIterator.hasNext()) {
                            line = playlistIterator.next();
                            streamUrl = baseUrl + line;
                            // System.out.println("Found URL '" + streamUrl
                            // + "' for seqNo. " + seqNo);
                            try {
                                streamData = loadStreamData(streamUrl);
                            } catch (Exception ex) {
                                logger.error("Caught exception: {}", ex.getMessage());
                                streamData = null;
                                // the while loop will keep going on,
                                // but it's just about 4 or 6 lines
                            }
                        }
                    }
                }
            }
            if ((streamData == null) && (streamUrl == null)) {
                // if streamUrl == null, then the JVM never got to the read the URL
                logger.error("Tried to get sequence no. {}, but didn't find it in the playlist. Am I too late?", seqNo);
            }
        }

        return streamData;
    }

    // example method from
    // https://www.codejava.net/java-se/networking/use-httpurlconnection-to-download-file-from-an-http-url
    // call: downloadFile(streamUrl,
    // "/Users/xyz/projects/video-tools/streaming-loadtest");
    private static final int BUFFER_SIZE = 4096;

    /**
     * Downloads a file from a URL
     * 
     * @param fileURL HTTP URL of the file to be downloaded
     * @param saveDir path of the directory to save the file
     * @throws IOException
     */
    public static void downloadFile(String fileURL, String saveDir)
            throws URISyntaxException, IOException {
        URL url = new URI(fileURL).toURL();
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = saveDir + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

}
