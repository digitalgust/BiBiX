/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.egls.client.audio;

import com.egls.client.BbApplication;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.mini.gui.GToolkit;
import org.mini.media.AudioDecoder;
import org.mini.media.AudioDevice;
import static org.mini.media.AudioDevice.getFormatBytes;
import org.mini.media.AudioFrameListener;
import org.mini.reflect.DirectMemObj;
import org.mini.zip.Zip;

/**
 *
 * @author Gust
 */
public class AudioMgr {

    static int format = AudioDevice.mal_format_s16;
    static int channels = 2;
    static int ratio = 22050;

    static AudioCallback listener;

    public static void setCallback(AudioCallback plistener) {

        listener = plistener;
    }

    /**
     * ========================================================================================================
     * playback
     * ========================================================================================================
     */
    public static final Integer BIBI = 0;
    public static final Integer DONG = 1;
    public static final Integer HUA = 2;
    public static final Integer MUSIC = 3;

    static Map<Integer, byte[]> audios = new HashMap();
    static AudioDevice playDevice;

    static public class AudioSource {

        public AudioSource(byte[] data, int pos) {
            this.rawdata = data;
            this.pos = pos;
        }

        public AudioSource(AudioDecoder decoder) {
            this.decoder = decoder;
        }

        public AudioDecoder decoder;
        public byte[] rawdata;
        public int pos;
        public AudioCallback callback;
    }

    static AudioFrameListener frameProcessor = new AudioFrameListener() {

        @Override
        public void onReceiveFrames(AudioDevice pDevice, int frameCount, DirectMemObj dmo) {
            int readNeed = frameCount * pDevice.channels;
            int bytes = readNeed * AudioDevice.getFormatBytes(capDevice.format);
            byte[] b = new byte[bytes];
            dmo.copyTo(b);
            //System.out.println("onSendFrames: " + frameCount + " , " + frameCount + "  b.len:" + b.length);
            try {
                baos.write(b);
            } catch (IOException ex) {
            }

            if (listener != null) {
                listener.onCapture((int) (System.currentTimeMillis() - capStartAt), b);
            }
        }

        @Override
        public int onSendFrames(AudioDevice pDevice, int frameCount, DirectMemObj dmo) {
            Object obj = pDevice.getUserData();
            if (obj == null) {
                return 0;
            } else {
                AudioSource src = (AudioSource) obj;
                if (src.decoder != null) {
                    AudioDecoder decoder = src.decoder;
                    int v = decoder.decode(pDevice, frameCount, dmo.getDataPtr());
                    //System.out.println("onsend: " + v);
                    if (v == 0) {
                        playDevice.setUserData(null);
                        if (src.callback != null) {
                            src.callback.onStop();
                        }
                    }
                    if (listener != null) {
                        listener.onPlay(0, null);
                    }
                    return v;
                } else if (src.rawdata != null) {

                    byte[] b = src.rawdata;
                    int bytes = dmo.getLength();
                    if (src.pos + bytes > b.length) {
                        bytes = b.length - src.pos;
                    }
                    //System.out.println("sent bytes: " + bytes + " pos: " + raw.pos);
                    dmo.copyFrom(src.pos, b, 0, bytes);
                    //printArr(b, pos);
                    src.pos += bytes;
                    if (src.pos >= b.length) {
                        playDevice.setUserData(null);
                        if (src.callback != null) {
                            src.callback.onStop();
                        }
                    }
                    if (listener != null) {
                        int time = (int) (src.pos / getDataTime(b));
                        listener.onPlay(time, null);
                    }
                    return bytes / getFormatBytes(pDevice.format) / pDevice.channels;
                }
            }
            return 0;
        }

        @Override
        public void onStop(AudioDevice pDevice) {
            if (listener != null) {
                listener.onStop();
            }
        }
    };

    public static byte[] readFile(String s) {
        try {
            File f = new File(BbApplication.getInstance().getSaveRoot() + s);
            byte[] b = new byte[(int) f.length()];

            FileInputStream dis = new FileInputStream(f);
            dis.read(b);
            dis.close();
            return b;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void loadAll() {
        byte[] b;
        b = GToolkit.readFileFromJar("/res/audio/bibi.flac");
        System.out.println("=========b:" + b);
        audios.put(BIBI, b);
        b = GToolkit.readFileFromJar("/res/audio/bibi.flac");
        audios.put(DONG, b);

    }

    public static void play(Integer code) {
        if (canPlay()) {
            byte[] b = audios.get(code);
            AudioDecoder decoder = new AudioDecoder(b, format, channels, ratio);
            playDecoder(decoder);
        }
    }

    public static void playData(byte[] data) {
        if (canPlay() && data != null) {
            playDevice.setUserData(new AudioSource(data, 0));
            System.out.println("total len:" + data.length);
            playImpl();
        }
    }

    /**
     * play flac , mp3 file rawdata
     *
     * @param decoder
     */
    public static void playDecoder(AudioDecoder decoder) {
        if (canPlay()) {
            playDevice.setUserData(new AudioSource(decoder));
            playImpl();
        }
    }

    public static boolean isPlaying(AudioDevice device) {
        return device.isStarted();
    }

    static private void playImpl() {

        playDevice.start();
    }

    static private boolean canPlay() {
        if (capDevice != null && capDevice.isStarted()) {
            return false;
        }
        if (playDevice == null) {
            playDevice = new AudioDevice(AudioDevice.mal_device_type_playback, format, channels, ratio);
            playDevice.setAudioFrameListener(frameProcessor);
        }
        return true;
    }

    public static void playStop() {
        if (playDevice != null && playDevice.isStarted()) {
            playDevice.stop();
            playDevice.destory();
        }
        playDevice = null;
    }
    /**
     * ========================================================================================================
     * capture
     * ========================================================================================================
     */

    static ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
    static byte[] capData;
    static byte[] capZipData;
    static AudioDevice capDevice = null;
    static long capStartAt;

    public static void captureStart() {

        playStop();
        captureStop();

        baos.reset();
        capData = null;
        capZipData = null;
        if (capDevice == null) {
            capDevice = new AudioDevice(AudioDevice.mal_device_type_capture, format, channels, ratio);
            capDevice.setAudioFrameListener(frameProcessor);
        }
        System.out.println("begin capture ");

        capStartAt = System.currentTimeMillis();
        capDevice.start();

    }

    public static void captureStop() {
        if (capDevice != null) {
            capStartAt = 0;
            listener = null;
            capDevice.stop();
            capDevice.destory();
            capDevice = null;
        }
    }

    public static byte[] getCaptureData() {
        if (capData == null && baos.size() > 0) {
            capData = baos.toByteArray();
        }
        return capData;
    }

    public static byte[] getCaptureZipData() {
        if (capZipData == null && baos.size() > 0) {
            capZipData = Zip.compress0(baos.toByteArray());
        }
        return capZipData;
    }

    public static void playCapAudio() {
        captureStop();

        byte[] b = getCaptureData();

        playData(b);
    }

    public static boolean isCapturing() {
        if (capDevice != null) {
            return capDevice.isStarted();
        }
        return false;
    }

    public static float getZipDataTime(byte[] zipdata) {
        if (zipdata == null) {
            return 0;
        }
        byte[] b = Zip.extract0(zipdata);
        return getDataTime(b);
    }

    public static float getDataTime(byte[] data) {
        return (float) data.length / channels / ratio / getFormatBytes(format);
    }
}
