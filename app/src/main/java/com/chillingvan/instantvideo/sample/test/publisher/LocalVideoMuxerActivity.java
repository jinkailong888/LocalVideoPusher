/*
 *
 *  *
 *  *  * Copyright (C) 2017 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.chillingvan.instantvideo.sample.test.publisher;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.instantvideo.sample.test.camera.CameraPreviewTextureView;
import com.chillingvan.instantvideo.sample.util.ScreenUtil;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.RTMPStreamMuxer;
import com.chillingvan.lib.publisher.CameraStreamPublisher;
import com.chillingvan.lib.publisher.StreamPublisher;

import java.io.IOException;
import java.util.List;

/**
 * This sample shows how to record a mp4 file. It cannot pause but only can restart. If you want to pause when recording, you need to generate a new file and merge old files and new file.
 */
public class LocalVideoMuxerActivity extends AppCompatActivity {

    private CameraStreamPublisher streamPublisher;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private Handler handler;
    private HandlerThread handlerThread;
    private TextView outDirTxt;
    private String outputDir;

    private MediaPlayerHelper mediaPlayer = new MediaPlayerHelper("mp4.mp4");
    private Surface mediaSurface;

    private IAndroidCanvasHelper drawTextHelper = IAndroidCanvasHelper.Factory.createAndroidCanvasHelper(IAndroidCanvasHelper.MODE.MODE_ASYNC);
    private Paint textPaint;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(ScreenUtil.dpToPx(getApplicationContext(), 15));

        outputDir = getExternalFilesDir(null) + "/test_mp4_encode.mp4";
//        outputDir = getExternalFilesDir(null) + "/test_flv_encode.flv";
        setContentView(R.layout.activity_test_mp4_muxer);
        cameraPreviewTextureView = findViewById(R.id.camera_produce_view);
        cameraPreviewTextureView.setOnDrawListener(new H264Encoder.OnDrawListener() {
            @Override
            public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
                //local
                GLTexture texture = producedTextures.get(0);
                GLTexture mediaTexture = producedTextures.get(0);

                SurfaceTexture outsideSurfaceTexture = texture.getSurfaceTexture();
                BasicTexture outsideTexture = texture.getRawTexture();
                int width = outsideTexture.getWidth();
                int height = outsideTexture.getHeight();
                SurfaceTexture mediaSurfaceTexture = mediaTexture.getSurfaceTexture();
                RawTexture mediaRawTexture = mediaTexture.getRawTexture();
                mediaRawTexture.setIsFlippedVertically(true);

                canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, width / 2, 0, width, height);

                mediaSurface = new Surface(texture.getSurfaceTexture());
            }

        });
        outDirTxt = (TextView) findViewById(R.id.output_dir_txt);
        outDirTxt.setText(outputDir);
        startButton = findViewById(R.id.test_camera_button);


        handlerThread = new HandlerThread("StreamPublisherOpen");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                playMedia();

                StreamPublisher.StreamPublisherParam streamPublisherParam = new StreamPublisher.StreamPublisherParam.Builder()
                        .setWidth(1080)
                        .setHeight(750)
                        .setVideoBitRate(1500 * 1000)
                        .setFrameRate(30)
                        .setIframeInterval(1)
                        .setSamplingRate(44100)
                        .setAudioBitRate(19200)
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .createStreamPublisherParam();
                streamPublisherParam.outputFilePath = outputDir;
                streamPublisherParam.outputUrl = "rtmp://18059.livepush.myqcloud.com/live/18059_GNxD1070222804584017920?bizid=18059&txSecret=afd6ccc5b9c72fd91ef98fc9061a7132&txTime=5C08D3C7";
                streamPublisherParam.setInitialTextureCount(1);
                streamPublisher.prepareEncoder(streamPublisherParam, new H264Encoder.OnDrawListener() {
                    @Override
                    public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
                        //remote
                        GLTexture texture = consumedTextures.get(0);
                        GLTexture mediaTexture = consumedTextures.get(0);

                        SurfaceTexture outsideSurfaceTexture = texture.getSurfaceTexture();
                        BasicTexture outsideTexture = texture.getRawTexture();
                        int width = outsideTexture.getWidth();
                        int height = outsideTexture.getHeight();
                        SurfaceTexture mediaSurfaceTexture = mediaTexture.getSurfaceTexture();
                        RawTexture mediaRawTexture = mediaTexture.getRawTexture();
                        mediaRawTexture.setIsFlippedVertically(true);

                        canvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, width / 2, height);
                    }

                });
                try {
                    streamPublisher.startPublish();
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.test_camera_button)).setText("START");
                        }
                    });
                }
            }
        };

//        streamPublisher = new CameraStreamPublisher(new MP4Muxer(), cameraPreviewTextureView);
        streamPublisher = new CameraStreamPublisher(new RTMPStreamMuxer(), cameraPreviewTextureView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        streamPublisher.resumeCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        streamPublisher.pauseCamera();
        if (streamPublisher.isStart()) {
            streamPublisher.closeAll();
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.release();
        }
        startButton.setText("START");
    }

    private void playMedia() {
        if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
            return;
        }

        mediaPlayer.playMedia(this, mediaSurface);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.release();
        }
        handlerThread.quitSafely();
    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (streamPublisher.isStart()) {
            streamPublisher.closeAll();
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.pause();
//            }
            textView.setText("START");
        } else {
            streamPublisher.resumeCamera();
            handler.sendEmptyMessage(1);
            textView.setText("STOP");
        }
    }
}
