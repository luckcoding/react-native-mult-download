package com.react_native_download;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.StatusUtil;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;
import com.react_native_download.utils.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class RNDownloadModule extends ReactContextBaseJavaModule {

    // context
    static ReactApplicationContext ctx;

    // document 路径
    private final static String DocumentDir = "DocumentDir";
    // 缓存路径
    private final static String CacheDir = "CacheDir";
    // Msg tag
    private static final String TAG = "<<RNDownload>>";

    // progress event name prefix
    private static final String ProgressEvent = "RNDownload_ProgressEvent_";
    private static final String FinishEvent = "RNDownload_FinishEvent_";
    private static final String InfoReadyEvent = "RNDownload_InfoReadyEvent_";

    private final Map<String, DownloadTask> taskMap = new HashMap();

    public RNDownloadModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);

        // bind context
        ctx = reactApplicationContext;
    }

    @Override
    public String getName() {
        return "RNDownload";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        ReactApplicationContext reactApplicationContext = this.getReactApplicationContext();

        // 导出配置项
        constants.put(DocumentDir, reactApplicationContext.getFilesDir().getAbsolutePath());
        constants.put(CacheDir, reactApplicationContext.getCacheDir().getAbsolutePath());

        return constants;
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");

            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (inputStream == null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "file to md5 failed", e);
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            buffer.append(Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return buffer.toString().toUpperCase();
    }

    /**
     * 发送js事件
     *
     */
    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(RCTNativeAppEventEmitter.class).emit(eventName, params);
    }

    public static File getParentFile(@NonNull Context context) {
        final File externalSaveDir = context.getExternalCacheDir();
        if (externalSaveDir == null) {
            return context.getCacheDir();
        } else {
            return externalSaveDir;
        }
    }

    private DownloadTask createTask(final ReadableMap params) {
        try {
            // 下载路径
            final URL uri = new URL(params.getString("uri"));
            String URL = uri.toString();

            // 命名
            final String filename = params.getString("filename");

            // 进度更新间隔
            int interval = params.getInt("interval");

            // 保存路径
            final String path = params.getString("path");
            final File parentFile = new File(path);

            // 跳过已完成的任务
            final Boolean skipCompleted = params.getBoolean("skipCompleted");

            return new DownloadTask.Builder(URL, parentFile)
                .setFilename(filename)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(interval)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(skipCompleted)
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 初始化
     * @param options
     * options.uri 下载路径
     * options.path 保存路径
     * options.filename 文件名
     * @param promise
     * promise.resolve(
     *      isFinish, 已完成
     *      taskId, 任务ID
     *      status, 任务状态
     *      info, 任务信息
     * )
     */
    @ReactMethod
    public void init(final ReadableMap options, Promise promise) {
        try {
            // new task
            DownloadTask task = createTask(options);
            if (task == null) {
                throw new Exception("Create DownloadTask Fail");
            }

            // callback result
            WritableMap result = Arguments.createMap();

            // status
            final StatusUtil.Status status = StatusUtil.getStatus(task);
            result.putString("status", status.toString());

            // is finish ??
            final Boolean isFinish = status == StatusUtil.Status.COMPLETED;
            result.putBoolean("isFinish", isFinish);

            // info
            final BreakpointInfo info = StatusUtil.getCurrentInfo(task);
            if (info != null) {
                result.putString("info", info.toString());
            }

            // 生成唯一任务id
            String taskId = Common.createUUID();
            taskMap.put(taskId, task);
            result.putString("taskId", taskId);

            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("ERROR", e);
        }
    }

    /**
     * start task
     * @param options
     * options.taskId 任务ID
     * @param promise
     * return promise
     */
    @ReactMethod
    public void start(final ReadableMap options, Promise promise) {
        final String taskId = options.getString("taskId");
        DownloadTask task = taskMap.get(taskId);

        if (task != null) {
            task.setTag("mark-task-started-" + taskId);

            // Map map = new HashMap();
            WritableMap args = Arguments.createMap();
            args.putString("taskId", taskId);

            startTask(args);
            promise.resolve(null);
        } else {
            promise.reject("ERROR","Never have this task: " + taskId);
        }
    }

    /**
     * cancel task
     */
    @ReactMethod
    public void cancel(final ReadableMap options, Promise promise) {
        final String taskId = options.getString("taskId");
        DownloadTask task = taskMap.get(taskId);

        if (task != null) {
            task.cancel();
            promise.resolve(null);
        } else {
            promise.reject("ERROR","Never have this task: " + taskId);
        }
    }

    /**
     * 检查任务是否在执行
     */
    @ReactMethod
    public void isStarted(final ReadableMap options, Promise promise) {
        final String taskId = options.getString("taskId");
        DownloadTask task = taskMap.get(taskId);

        final boolean started = task.getTag() != null;

        if (task != null) {
            promise.resolve(started);
        } else {
            promise.reject("ERROR","Never have this task: " + taskId);
        }
    }

    private void startTask(@NonNull WritableMap options) {
        final String taskId = options.getString("taskId");

        DownloadTask task = taskMap.get(taskId);

        task.enqueue(new DownloadListener4WithSpeed() {
            private long totalLength;
            private String readableTotalLength;

            @Override
            public void taskStart(@NonNull DownloadTask downloadTask) {
                Log.e(TAG, "DownloadTask: taskStart");
            }

            @Override
            public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {
                Log.e(TAG, "DownloadTask: connectStart: " + blockIndex);
            }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
                Log.e(TAG, "DownloadTask: connectEnd: " + blockIndex);
            }

            @Override
            public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint, @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
                totalLength = info.getTotalLength();
                readableTotalLength = Util.humanReadableBytes(totalLength, true);

                /**
                 * send infoReady event
                 */
                WritableMap data = Arguments.createMap();
                data.putDouble("totalLength", totalLength);
                data.putString("readableTotalLength", readableTotalLength);
                sendEvent(getReactApplicationContext(), InfoReadyEvent + taskId, data);
            }

            @Override
            public void progressBlock(@NonNull DownloadTask task, int blockIndex, long currentBlockOffset, @NonNull SpeedCalculator blockSpeed) {

            }

            @Override
            public void progress(@NonNull DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed) {
                final String readableOffset = Util.humanReadableBytes(currentOffset, true);
                final String progressStatus = readableOffset + "/" + readableTotalLength;
                final String speed = taskSpeed.speed();
                final String progressStatusWithSpeed = progressStatus + "(" + speed + ")";

                Log.e(TAG, "DownloadTask: progress: " + progressStatusWithSpeed);

                /**
                 * send progress event
                 */
                WritableMap data = Arguments.createMap();
                data.putString("speed", speed);
                data.putString("total", readableTotalLength);
                data.putString("readable", readableOffset);
                sendEvent(getReactApplicationContext(), ProgressEvent + taskId, data);
            }

            @Override
            public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info, @NonNull SpeedCalculator blockSpeed) {

            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @android.support.annotation.Nullable Exception realCause, @NonNull SpeedCalculator taskSpeed) {
                final String statusWithSpeed = cause.toString() + " " + taskSpeed.averageSpeed();
                Log.e(TAG, "DownloadTask: taskEnd(statusWithSpeed): " + statusWithSpeed);

                WritableMap data = Arguments.createMap();
//                data.putString("path", params.getString("path"));

                sendEvent(getReactApplicationContext(), FinishEvent + taskId, data);

                task.setTag(null);

//                promise.resolve(data);
//                if (cause == EndCause.CANCELED) {
//                    final String realMd5 = fileToMD5(task.getFile().getAbsolutePath());
//                    if (!realMd5.equalsIgnoreCase("f836a37a5eee5dec0611ce15a76e8fd5")) {
//                        Log.e(TAG, "file is wrong because of md5 is wrong " + realMd5);
//                    }
//                }
            }
        });
    }
}