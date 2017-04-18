Medoly Last.fm Scrobbler Plugin
===============================

Last.fm plugin app for Medoly.

## Description

Medoly Last.fm Scrobbler Plugin は、[Medoly](https://play.google.com/store/apps/details?id=com.wa2c.android.medoly) で再生しているメディアの再生情報を [Last.fm](http://www.last.fm/) に送信 ( Scrobble ) するAndroidアプリです。
[Google Play](https://play.google.com/store/apps/details?id=com.wa2c.android.medoly.plugin.action.lastfm)で配信中。

## Requirement

* Android Studio

## Build

1. プロジェクトのクローンを作成します。

2. Android Studio にプロジェクトをインポートします。

3. 以下のディレクトリに存在する Token.java.rename ファイルを、Token.java にリネームします。
    src/main/java/com/wa2c/android/medoly/plugin/action/lastfm/

4. Android Studio でビルドします。

* 実際に使用するためには、Last.fm のサイト (<http://www.last.fm/>) から、API key および API Secret を取得し、Token.javaに入力する必要があります。
  現在のアプリで使用している値は非公開とさせてください。

## Usage

* 使い方については、[Google Play](https://play.google.com/store/apps/details?id=com.wa2c.android.medoly.plugin.action.lastfm) のページを参考にしてください。

## Licence

[MIT](https://opensource.org/licenses/MIT)

## Author

[wa2c](https://bitbucket.org/wa2c/)
