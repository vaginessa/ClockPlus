package com.philliphsu.clock2.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.internal.$Gson$Types;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by maohl on 2017/4/26.
 * 对OKHttp简单的封装
 * compile 'com.squareup.okhttp3:okhttp:3.2.0'
 */

public class OKHttp3Util {
    private static final String TAG = "OKHttp3Util";
    private static OKHttp3Util mInstance; //当前实例
    private OkHttpClient mOkHttpClient;
    private Handler mDelivery;
    private Gson mGson;
    private Context context1;
    //Tip：key是String类型且为url的host部分
    private HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
    //get请求
    private GetDelegate mGetDelegate = new GetDelegate();
    //post请求
    private PostDelegate mPostDelegate = new PostDelegate();
    //上传文件的模块
    private UploadDelegate mUploadDelegate = new UploadDelegate();

    public UploadDelegate getmUploadDelegate() {
        return mUploadDelegate;
    }

    public PostDelegate getmPostDelegate() {
        return mPostDelegate;
    }

    public GetDelegate getmGetDelegate() {
        return mGetDelegate;
    }


    private OKHttp3Util() {
        mOkHttpClient = new OkHttpClient.Builder().writeTimeout(30L, TimeUnit.SECONDS).connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS)
                // TODO: 2017/4/26  设置cookie
                /**
                 * OkHttp可以不用我们管理Cookie，自动携带，保存和更新Cookie。
                 * 方法是在创建OkHttpClient设置管理Cookie的CookieJar：
                 *这样以后发送Request都不用管Cookie这个参数也不用去response获取新Cookie什么的了。
                 * 还能通过cookieStore获取当前保存的Cookie。
                 */
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<Cookie>();
                    }
                })
                .retryOnConnectionFailure(false)//连接失败后是否重新连接
                .build();

//        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        //要刷新UI，handler要用到主线程的looper。那么在主线程 Handler handler = new Handler();
        // 如果在其他线程，也要满足这个功能的话，要Handler handler = new Handler(Looper.getMainLooper());
        mDelivery = new Handler(Looper.getMainLooper());
        mGson = new Gson();
    }

    public static OKHttp3Util getInstance() {
        if (mInstance == null) {
            synchronized (OKHttp3Util.class) {
                if (mInstance == null) {
                    mInstance = new OKHttp3Util();
                }
            }
        }
        return mInstance;
    }

    //参数
    public static class Param {

        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key;
        String value;
    }

    //get请求接口
    public static void getAsyn(String url, ResultCallback callback) {
        getInstance().getmGetDelegate().getAsyn(url, callback);
    }

    public static void getAsyn(String url, ResultCallback callback, Object tag) {
        getInstance().getmGetDelegate().getAsyn(url, null, callback, tag);
    }

    public static void getAsyn(String url, String token, ResultCallback callback) {
        getInstance().getmGetDelegate().getAsyn(url, token, callback, null);
    }

    public static void getAsyn(String url, String token, ResultCallback callback, Object tag) {
        getInstance().getmGetDelegate().getAsyn(url, token, callback, tag);
    }

    //post请求接口
    public static void postAsyn(String url, String params, final ResultCallback callback) {
        getInstance().getmPostDelegate().postAsyn(url, params, callback, null);
    }

    public static void postAsyn(String url, String params, final ResultCallback callback, Object tag) {
        getInstance().getmPostDelegate().postAsyn(url, params, callback, tag);
    }

    //添加了一个请求头
    public static void postAsyn(String url, String token, String params, final ResultCallback callback) {
        getInstance().getmPostDelegate().postAsyn(url, token, params, callback, null);
    }

    public static void postAsyn(String url, String token, String params, final ResultCallback callback, Object tag) {
        getInstance().getmPostDelegate().postAsyn(url, token, params, callback, tag);
    }

    //上传文件 传递参数
    public static UploadDelegate getUploadDelegate() {
        return getInstance().getmUploadDelegate();
    }

    //*******************GET请求模块开始********************************************
    public class GetDelegate {
        private Request buildGetRequest(String url, String token, Object tag) {
            Request.Builder builder = new Request.Builder()
//                    .addHeader("Authorization","Bearer "+SPrefUtil.getString(getInstance().context1,"token","eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJNZW1iZXJJZCI6IjdiMzAzZmYzLTk1ZGItNDA2Mi1iY2JmLTBjOTI2ZTdjMGNhMCIsInN1YiI6IjdiMzAzZmYzLTk1ZGItNDA2Mi1iY2JmLTBjOTI2ZTdjMGNhMCIsImp0aSI6IjZmMjc5Y2ExLTU4ZGItNDEwOC05NjEzLWZhMDUyZGY2MzUyOSIsImlhdCI6IjIwMTcvOS8yMSA3OjU4OjIzIiwibmJmIjoxNTA1OTgwNzAzLCJleHAiOjE1MDYwNjcxMDMsImlzcyI6IkZNSlMiLCJhdWQiOiJGTUpTIn0.DTEJzHvEs5ZDh3kBjUxUjI4fGin53-1jEgIArOs8340"))
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json;charset=utf-8")
                    .addHeader("Connection", "close")
                    .url(url);
            if (tag != null) {
                builder.tag(tag);
            }
            if (!TextUtils.isEmpty(token)) {
                builder.header("Token", token);
            }
            return builder.build();
        }

        //异步的get请求
        public void getAsyn(String url, final ResultCallback callback) {
            getAsyn(url, null, callback, null);
        }

        public void getAsyn(String url, String token, final ResultCallback callback, Object tag) {
            final Request request = buildGetRequest(url, token, tag);
            getAsyn(request, callback);
        }

        public void getAsyn(Request request, ResultCallback callback) {
            deliveryResult(callback, request);
        }
    }
    //*******************GET请求模块结束********************************************

    ////    *******************POST请求模块开始********************************************
//    public class PostDelegate1{
//        private Request buildPostFormRequest(String url, String token,Param[] params, Object tag) {
//            if (params == null) {
//                params = new Param[0];
//            }
//            FormBody.Builder builder = new FormBody.Builder();
//            for (Param param : params) {
//                builder.add(param.key, param.value);
//            }
//            RequestBody requestBody = builder.build();
//
//            Request.Builder reqBuilder = new Request.Builder();
//            reqBuilder.url(url)
//                    .addHeader("Content-Type","application/json;charset=utf-8")
//                    .addHeader("Connection", "close")
//                    .addHeader("Content-Encoding","gzip")
//                    .addHeader("Date","Tue, 19 Sep 2017 06:06:26 GMT")
//                    .addHeader("Server","nginx/1.10.2-upupw")
//                    .addHeader("Transfer-Encoding","chunked")
//                    .addHeader("Vary","Accept-Encoding")
//                    .post(requestBody);
//
//            if (tag != null) {
//                reqBuilder.tag(tag);
//            }
//
//            if (!TextUtils.isEmpty(token)){
//                reqBuilder.header("Token",token);
//            }
//            return reqBuilder.build();
//        }
//
//
//         //异步的post请求
//         public void postAsyn(String url,Param[] params, final ResultCallback callback, Object tag) {
//             Request request = buildPostFormRequest(url,null,params, tag);
//             deliveryResult(callback, request);
//         }
//
//        public void postAsyn(String url, String token,Param[] params, final ResultCallback callback, Object tag) {
//            Request request = buildPostFormRequest(url, token,params, tag);
//            deliveryResult(callback, request);
//        }
//        public void postAsyn(String url, Map<String, String> params, final ResultCallback callback, Object tag) {
//            Param[] paramsArr = map2Params(params);
//            postAsyn(url,paramsArr, callback, tag);
//        }
//        public void postAsyn(String url, String token,Map<String, String> params, final ResultCallback callback, Object tag) {
//            Param[] paramsArr = map2Params(params);
//            postAsyn(url, token,paramsArr, callback, tag);
//        }
//    }
    public class PostDelegate {
        private Request buildPostFormRequest(String url, String token, String params, Object tag) {
//                if (params == null) {
//                    params = new Param[0];
//                }
//                FormBody.Builder builder = new FormBody.Builder();
//                for (Param param : params) {
//                    builder.add(param.key, param.value);
//                }
//                RequestBody requestBody = builder.build();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, params);
            Request.Builder reqBuilder = new Request.Builder();
            if (tag != null) {
                reqBuilder.tag(tag);
            }
            if (!TextUtils.isEmpty(token)) {
                reqBuilder.header("Token", token);
            }
            reqBuilder.url(url)
                    .addHeader("Content-Type", "application/json;charset=utf-8")
                    .addHeader("Connection", "close")
                    .addHeader("Authorization", "Bearer " + token)
//                    .addHeader("Content-Encoding", "gzip")
//                    .addHeader("Date", "Tue, 19 Sep 2017 06:06:26 GMT")
//                    .addHeader("Server", "nginx/1.10.2-upupw")
//                    .addHeader("Transfer-Encoding", "chunked")
//                    .addHeader("Vary", "Accept-Encoding")
                    .post(requestBody);

            return reqBuilder.build();
        }

        public void postAsyn(String url, String params, final ResultCallback callback, Object tag) {
            Request request = buildPostFormRequest(url, null, params, tag);
            deliveryResult(callback, request);
        }

        public void postAsyn(String url, String token, String params, final ResultCallback callback, Object tag) {
            Request request = buildPostFormRequest(url, token, params, tag);
//            Log.i("OKHttp3Util postAsyn", "url:" + url + " token:" + token + " params:" + params);
            deliveryResult(callback, request);
        }
    }
    //*******************POST请求模块结束********************************************

    //*******************上传文件请求模块开始********************************************
    public class UploadDelegate {

        private Request buildMultipartFormRequest(String url, File[] files,
                                                  String[] fileKeys, Param[] params, Object tag) {
            //检查参数是否为空
            params = validateParam(params);
            /* form的分割线,自己定义 */
            String boundary = "xx--------------------------------------------------------------xx";
            MultipartBody.Builder mBody = new MultipartBody
                    .Builder(boundary)
                    .setType(MultipartBody.FORM);
            for (Param param : params) {
                mBody.addFormDataPart(param.key, param.value);
            }
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), files[i]);
                    mBody.addFormDataPart(fileKeys[i], files[i].getName(), fileBody);
                }
            }
            RequestBody requestBody = mBody.build();
            return new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .tag(tag)
                    .build();
        }

        private Param[] validateParam(Param[] params) {
            if (params == null)
                return new Param[0];
            else return params;
        }

        //异步基于post的文件上传，单文件且携带其他form参数上传
        public void postAsyn(String url, String fileKey, File file, Map<String, String> params, ResultCallback callback) {
            postAsyn(url, new String[]{fileKey}, new File[]{file}, params, callback, null);
        }

        public void postAsyn(String url, String fileKey, File file, Param[] params, ResultCallback callback) {
            postAsyn(url, new String[]{fileKey}, new File[]{file}, params, callback, null);
        }

        public void postAsyn(String url, String fileKey, File file, Param[] params, ResultCallback callback, Object tag) {
            postAsyn(url, new String[]{fileKey}, new File[]{file}, params, callback, tag);
        }

        //多文件上传（一个文件名对应一个文件）
        public void postAsyn(String url, String[] fileKeys, File[] files, Map<String, String> params, ResultCallback callback) {
            Param[] paramsArr = map2Params(params);
            postAsyn(url, fileKeys, files, paramsArr, callback, null);
        }

        public void postAsyn(String url, String[] fileKeys, File[] files, Map<String, String> params, ResultCallback callback, Object tag) {
            Param[] paramsArr = map2Params(params);
            postAsyn(url, fileKeys, files, paramsArr, callback, tag);
        }

        public void postAsyn(String url, String[] fileKeys, File[] files, Param[] params, ResultCallback callback) {
            postAsyn(url, fileKeys, files, params, callback, null);
        }

        public void postAsyn(String url, String[] fileKeys, File[] files, Param[] params, ResultCallback callback, Object tag) {
            Request request = buildMultipartFormRequest(url, files, fileKeys, params, tag);
            deliveryResult(callback, request);
        }
    }

    //回掉函数
    public static abstract class ResultCallback<T> {
        Type mType;

        public ResultCallback() {
            mType = getSuperclassTypeParameter(getClass());
        }

        //得到泛型T的实际Type
        static Type getSuperclassTypeParameter(Class<?> subclass) {
            Type superclass = subclass.getGenericSuperclass();
            if (superclass instanceof Class) {
                throw new RuntimeException("Missing type parameter.");
            }
            ParameterizedType parameterized = (ParameterizedType) superclass;
            return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
        }

        public void onBefore(Request request) {
        }

        public void onAfter() {
        }

        public abstract void onError(Request request, Exception e);

        public abstract void onResponse(T response);
    }


    private final ResultCallback<String> DEFAULT_RESULT_CALLBACK = new ResultCallback<String>() {
        @Override
        public void onError(Request request, Exception e) {

        }

        @Override
        public void onResponse(String response) {

        }
    };

    //通用的异步请求回掉
    private void deliveryResult(ResultCallback callback, final Request request) {
        if (callback == null) callback = DEFAULT_RESULT_CALLBACK;
        final ResultCallback resCallBack = callback;
        //UI thread 主线程 开始请求数据
        callback.onBefore(request);
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedStringCallback(request, e, resCallBack);
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final String string = response.body().string();
//                    KLog.e("请求结果", "结果为:" + string);
                    if (resCallBack.mType == String.class) {
                        sendSuccessResultCallback(string, resCallBack);
                    } else {
                        Object o = mGson.fromJson(string, resCallBack.mType);
                        sendSuccessResultCallback(o, resCallBack);
                    }
                    call.cancel();
                } catch (IOException e) {
                    sendFailedStringCallback(response.request(), e, resCallBack);
                } catch (com.google.gson.JsonParseException e)//Json解析的错误
                {
                    sendFailedStringCallback(response.request(), e, resCallBack);
                }
            }
        });
    }

    //发送请求失败信息
    private void sendFailedStringCallback(final Request request, final Exception e, final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(request, e);
                callback.onAfter();
            }
        });
    }

    //发送请求成功信息
    private void sendSuccessResultCallback(final Object object, final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                callback.onResponse(object);
                callback.onAfter();
            }
        });
    }

    private Param[] map2Params(Map<String, String> params) {
        if (params == null) return new Param[0];
        int size = params.size();
        Param[] res = new Param[size];
        Set<Map.Entry<String, String>> entries = params.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            res[i++] = new Param(entry.getKey(), entry.getValue());
        }
        return res;
    }

    //取消所有的请求
    public static void cancelAllTag() {
        getInstance()._cancelTag();
    }

    private void _cancelTag() {
        mOkHttpClient.dispatcher().cancelAll();
    }

    //根据tag取消请求
    public static void cancleByTag(Object tag) {
        getInstance()._cancleByTag(tag);
    }

    private void _cancleByTag(Object tag) {
        Dispatcher dispatcher = mOkHttpClient.dispatcher();
        synchronized (dispatcher) {
            for (Call call : dispatcher.queuedCalls()) {
                if (tag.equals(call.request().tag())) {
                    call.cancel();
                }
            }
            for (Call call : dispatcher.runningCalls()) {
                if (tag.equals(call.request().tag())) {
                    call.cancel();
                }
            }
        }
    }

}
