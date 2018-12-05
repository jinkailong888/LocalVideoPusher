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

package com.chillingvan.instantvideo.sample;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ling on 16-3-27.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    public static String CRASH_PATH;
    private static CrashHandler INSTANCE = new CrashHandler();
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static CrashHandler init(Context applicationContext) {
        if (mContext == null) {
            mContext = applicationContext;
            CRASH_PATH = mContext.getExternalFilesDir(null) + File.separator + "crash";
        }
        return INSTANCE;
    }

    private static String convertYYMMDDHHmm(long time) {
        Date date = new Date(time);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        return dateFormat.format(date);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        handleException(ex);
        mDefaultHandler.uncaughtException(thread, ex);
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        saveErrorInfo(ex);

        return true;
    }

    private void saveErrorInfo(Throwable ex) {
        File path = new File(CRASH_PATH);
        if (!path.exists()) {
            path.mkdirs();
        }

        File file = new File(CRASH_PATH + File.separator + "log_" + convertYYMMDDHHmm(System.currentTimeMillis()) + ".txt");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if (!file.exists()) {
                file.createNewFile();
            }
            Writer writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            ex.printStackTrace(pw);
            pw.close();
            String error = writer.toString();
            fos.write(error.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
