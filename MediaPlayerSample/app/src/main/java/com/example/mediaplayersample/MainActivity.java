package com.example.mediaplayersample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    private final int REQUEST_CODE_READ_STRAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        List<String> paths = getAudioDataFromDevice();

        playMusic(paths);
    }

    /**
     * 端末内のストレージにアクセスするため、permissionが必要。
     * 手順
     * 1. AndroidManifestにpermissionを追加する
     * 2. Activity起動時（onCreate内）でパーミッションを確認し、なければ{@link #requestPermissions}を呼ぶ。
     */
    private void checkPermission() {
        String readStorage = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(readStorage) == PackageManager.PERMISSION_DENIED) {
            // 第一引数は配列のため、複数のパーミッションを一度にリクエストすることも可能。
            requestPermissions(new String[]{readStorage}, REQUEST_CODE_READ_STRAGE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * パーミッションが許可された場合は、このonActivityResultにRESULT_OKが帰ってくる。
         *  OKでない場合は再度requestPermissionsを呼んでも良いし、Toastでメッセージを表示させるだけでもどちらでも良い。
         *  アプリの機能的に、ストレージアクセスは必要不可欠なのでどこかのタイミングで必ず許可させたい。
         */
        if (requestCode == REQUEST_CODE_READ_STRAGE && resultCode != RESULT_OK) {
            Toast.makeText(this, "設定からこのアプリのパーミッションを許可してください。", Toast.LENGTH_SHORT).show();
        }
    }


    private List<String> getAudioDataFromDevice() {
        /**
         * ContentResolverは、端末のストレージにアクセスするときに使うモノとして認識してOK。
         * {ContentResolver.query()}を使うと、画像や動画、音楽ファイル等色々なファイルを取得できる。
         * 少し難しいけど　https://developer.android.com/guide/topics/providers/content-provider-basics?hl=ja#top_of_page　に詳しく書いてるよ
         *          */
        ContentResolver contentResolver = getContentResolver();
        /**
         * {ContentResolver.query()}を呼ぶと返ってくるCursorは、
         * 簡単に言うと端末のストレージから取得した情報の一覧を持っている。
         */
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null);

        List<String> paths = new ArrayList<>();
        /**
         * {ContentResolver.query()}が
         * 途中でエラーが起きたらnullが返ってくるのと、端末に情報がなかったら0が返ってくる。
         */
        if (cursor != null && cursor.getCount() != 0) {
            /**
             * cursorは情報の一覧を持っている塊なので、moveToNext()を呼んでどんどん次の情報を読み込む。
             * moveToNext()は、Cursorの中身が何もなくなったらfalseを返すため、
             * moveToNext()がtrueの間にデータを取り出す。
             */
            while (cursor.moveToNext()) {
                /**
                 * Cursorは、情報を表としてもっている。
                 * ID | Title   | total time |               _data(path)                  |
                 * 0  | タイトル |     10     |   /storage/emulated/0/Download/loop1.mp3   |
                 * みたいな。適当だけど。
                 * 今回はpathが欲しいため、pathが何列目かを{cursor.getColumnIndex()}で取得している。
                 */
                int index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                /**
                 * 先程取得した列番号を元に、pathを取り出す。
                 */
                String path = cursor.getString(index);
                paths.add(path);
                Log.d("MYTAG", path);
            }
            /**
             * 必ず閉める
             */
            cursor.close();
        }
        return paths;
    }

    /**
     * 今は仮に0番目のpathをそのまま再生しているけど、
     * MusicPlayerなら次へを押した際に次の配列の音楽を再生するようにしたほうがいいかも
     * @param paths ストレージから読み込んだpathたち。
     */
    private void playMusic(List<String> paths) {
        mediaPlayer = MediaPlayer.create(this, Uri.parse(paths.get(0)));
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.start();
    }

    /**
     * 必ず開放する。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }
}
