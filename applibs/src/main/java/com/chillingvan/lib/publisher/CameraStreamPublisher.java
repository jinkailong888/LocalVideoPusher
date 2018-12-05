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

package com.chillingvan.lib.publisher;

import android.graphics.SurfaceTexture;

import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.IMuxer;

import java.io.IOException;
import java.util.List;

/**
 * Data Stream:
 * Camera -> SurfaceTexture of GLSurfaceTextureProducerView -> Surface of MediaCodec -> encode data(byte[]) -> RTMPMuxer -> Server
 */

public class CameraStreamPublisher {

    private StreamPublisher streamPublisher;
    private IMuxer muxer;
    private GLMultiTexProducerView cameraPreviewTextureView;

    public CameraStreamPublisher(IMuxer muxer, GLMultiTexProducerView cameraPreviewTextureView) {
        this.muxer = muxer;
        this.cameraPreviewTextureView = cameraPreviewTextureView;
    }

    private void initCameraTexture() {
        cameraPreviewTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                streamPublisher = new StreamPublisher(eglContext, muxer);
            }
        });
        cameraPreviewTextureView.setSurfaceTextureCreatedListener(new GLMultiTexProducerView.SurfaceTextureCreatedListener() {
            @Override
            public void onCreated(List<GLTexture> producedTextureList) {
                GLTexture texture = producedTextureList.get(0);
                SurfaceTexture surfaceTexture = texture.getSurfaceTexture();
                streamPublisher.addSharedTexture(new GLTexture(texture.getRawTexture(), surfaceTexture));
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraPreviewTextureView.requestRenderAndWait();
                        streamPublisher.drawAFrame();
                    }
                });
            }
        });
    }

    public void prepareEncoder(StreamPublisher.StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        streamPublisher.prepareEncoder(param, onDrawListener);
    }

    public void resumeCamera() {
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }

    public boolean isStart() {
        return streamPublisher != null && streamPublisher.isStart();
    }

    public void pauseCamera() {
        cameraPreviewTextureView.onPause();
    }

    public void startPublish() throws IOException {
        streamPublisher.start();
    }


    public void closeAll() {
        streamPublisher.close();
    }
}
