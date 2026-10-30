開発者が個人用に作成したツールのため、利用はお勧めしません。

# sss-wallpaper

Windowsのモニターごとに、別の壁紙を設定するJavaFXアプリです。

画像ファイルをモニターの枠へドロップすると、すぐにPCの壁紙へ反映されます。画像は縦横比を保ってFitし、左寄せか右寄せを選べます。余白を埋める色は、白か黒を選べます。

## 使い方

- **画像ファイルのドロップ：** 画像ファイルをモニターの枠へドロップすると、その画像が壁紙になります。使える形式は、jpg、jpeg、png、bmpです。
- **フォルダのドロップ：** フォルダをモニターの枠へドロップすると、そのモニターのフォルダとして登録されます。登録すると、フォルダの中から画像を1枚ランダムに選んで、すぐに壁紙にします。登録したフォルダの名前は、枠の上に表示されます。
- **ランダム変更：** 「Randomize all」ボタンを押すと、フォルダを登録した全モニターの壁紙が、それぞれのフォルダからランダムに変わります。フォルダのないモニターの壁紙は変わりません。
- **寄せ方と埋める色：** 枠の下のComboBoxで選びます。変えると、すぐに壁紙を作り直します。
- **場所を開く：** 枠の「Open location」ボタンを押すと、エクスプローラーで場所を開きます。フォルダを登録したモニターではそのフォルダを、画像を設定したモニターでは画像のあるフォルダを開きます。

フォルダの中のサブフォルダは見ません。フォルダの直下にある画像だけを使います。

設定は`~/.sss-wallpaper/settings.properties`に保存されます。次に起動したときも、同じ設定が使われます。

## 必要なもの

- Windows 10以降

ソースからビルドするときは、次のものも必要です。

- JDK 25以降
- Maven

実行ファイルを作るときは、さらに次のものも必要です。

- GraalVM CE 25.4
- Visual StudioのC++のビルドツール

## インストール

1. [GitHubのReleases](https://github.com/sosuisen/sss-wallpaper/releases)から、`sss-wallpaper-<バージョン>.zip`をダウンロードします。
2. zipを、好きな場所へ展開します。中には、`sss-wallpaper.exe`といくつかの`dll`があります。
3. `sss-wallpaper.exe`を実行します。

`exe`は、同じフォルダの`dll`を使います。`exe`だけを取り出しても動きません。

GraalVMのnative imageで作った実行ファイルです。JDKを入れる必要はありません。

## 実行

```bash
mvn javafx:run
```

`--randomize`オプションを付けると、ウィンドウを出さずに起動します。登録フォルダから画像をランダムに選び、全モニターの壁紙を変えて終了します。失敗したときは、エラーダイアログを表示します。ダイアログを閉じると、終了コード1で終わります。

```bash
mvn javafx:run -Djavafx.args="--randomize"
```

## ログオン時にランダム変更する

スタートアップフォルダにショートカットを置くと、Windowsへのサインイン時に壁紙をランダムに変えられます。

1. インストールした`sss-wallpaper.exe`を右クリック →「その他のオプションを確認」→「ショートカットの作成」を選びます。
2. `Win`＋`R`で`shell:startup`を開きます。これがスタートアップフォルダです。
3. 作ったショートカットを、スタートアップフォルダへ移します。
4. ショートカットを右クリック →「プロパティ」→「ショートカット」タブの「リンク先」の末尾に、`--randomize`を追記します。パスにスペースがある場合は、パス部分だけ引用符で囲みます。

```text
"C:\Users\you\Apps\sss-wallpaper\sss-wallpaper.exe" --randomize
```

ウィンドウは開きません。壁紙を変えると、すぐに終了します。

先に、ウィンドウありで起動して、各モニターにフォルダを登録しておいてください。フォルダのないモニターの壁紙は変わりません。

## 壁紙の設定の保存先

このアプリは、レジストリを直接読み書きしません。壁紙の設定には、WindowsのAPIの`IDesktopWallpaper`を使います。レジストリとファイルの更新は、Windows自身が行います。設定画面で壁紙を変えたときと同じ処理です。

アプリがファイルを書くのは、`~/.sss-wallpaper/`の下だけです。

| 場所 | 内容 |
|---|---|
| `~/.sss-wallpaper/settings.properties` | モニターごとの設定 |
| `~/.sss-wallpaper/wallpapers/` | 合成した壁紙の画像 |

Windowsは、壁紙の設定を次の場所に保存します。

| 場所 | 内容 |
|---|---|
| `HKCU\Control Panel\Desktop` | 壁紙の画像のパスと、表示のしかた。`TranscodedImageCache_000`などに、モニターごとの画像のパスが入る |
| `%APPDATA%\Microsoft\Windows\Themes\` | Windowsがコピーした壁紙の画像。`Transcoded_000`などがモニターごとの画像 |
| `HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Wallpapers` | 設定画面の「最近使った画像」の履歴 |

Windowsは画像をコピーして使います。そのため、`~/.sss-wallpaper/wallpapers/`の画像を消しても、壁紙は消えません。

## テスト

```bash
mvn test
```

`@Tag("desktop")`のテストは、実際のPCの壁紙を一時的に変えます。そのため、通常のテストからは外しています。手動で実行するときは、次のコマンドを使います。

```bash
mvn test -Pdesktop
```

`mvn test`を実行すると、JaCoCoでコードカバレッジも測ります。レポートは`target/site/jacoco/index.html`に出力されます。OSのAPIを直接呼ぶクラスと起動用のクラスは、測定から外しています。対象のクラスは`pom.xml`に書いてあります。

ミューテーションテストには、PITを使います。PITは、コードを少しずつ壊した版を作り、テストがその変化に気づくかを調べます。時間がかかるので、`mvn test`とは別に実行します。

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

レポートは`target/pit-reports/index.html`に出力されます。

## 実行ファイルを作る

GraalVMのnative imageで、JVMなしで動く実行ファイルを作ります。起動を速くするためです。この判断の理由は、[ADR 2](docs/adr/adr-002-native-image-only-distribution.md)にあります。

`JAVA_HOME`にGraalVM CE 25.4を指定して、`native`プロファイルでビルドします。

```bash
JAVA_HOME=<GraalVM CE 25.4のフォルダ> mvn -Pnative package
```

- 作られる場所は`target/`です。
- 実行ファイルは`target/sss-wallpaper.exe`です。
- 使うときは、`exe`と、同じ場所にできた`dll`をまとめてコピーします。`exe`だけをコピーしても動きません。

`mvn clean`をすると、`target`の中は消えます。そのため、コピーしてから使ってください。

`native`プロファイルを付けない`mvn package`では、実行ファイルを作りません。

### メタデータ

native imageには、リフレクション、リソース、FFMの呼び出しを、ビルド時に知らせる必要があります。その情報は`src/main/resources/META-INF/native-image/com.sosuisha/sss-wallpaper/reachability-metadata.json`にあります。

知らせ忘れても、ビルドは通ります。失敗するのは、実行ファイルを実行したときです。そのため、次のときは、このファイルを作り直します。

- FFMで、引数や戻り値の型の組み合わせが新しい関数を呼ぶ。
- CSSや画像など、新しいリソースを読む。
- 新しいJavaFXのコントロールを使う。
- JavaFXやAtlantaFXの版を上げる。

作り直すには、GraalVMのJVMに、トレーシングエージェントを付けてアプリを起動します。手順は[ネイティブビルドの調査](docs/native-image-research.md)の「トレーシングエージェント」にあります。次の4つの経路を通します。

- ウィンドウありの起動。ドロップ、壁紙の設定、「Open location」を操作します。
- `--randomize`での起動
- ウィンドウありの起動でのエラーダイアログ。`-Dsss.settings.file`にフォルダを指定すると、設定の読み込みが失敗します。
- `--randomize`での起動でのエラーダイアログ。同じく`-Dsss.settings.file`にフォルダを指定します。

### アイコン

実行ファイルのアイコンは、`images/icon.ico`です。`src/windows/sss-wallpaper.rc`で指定し、コンパイルした`src/windows/sss-wallpaper.res`をリンカーに渡して埋めます。

`.res`はコミットしてあります。コンパイルに使う`rc.exe`は、ふつうのシェルにもCIにもPATHが通っていないためです。アイコンを変えたときは、Visual StudioのDeveloper Command Promptで、次のように作り直します。

```bat
cd src\windows
rc /fo sss-wallpaper.res sss-wallpaper.rc
```

## リリース

アプリのバージョンは、Gitのタグで決めます。`pom.xml`のバージョンは書き換えません。

リリースするときは、`v`で始まるタグを打って、pushします。

```bash
git tag v1.2.0
git push origin v1.2.0
```

タグをpushすると、GitHub Actionsの`release.yml`が動きます。

1. タグの名前から先頭の`v`を取り、`1.2.0`をバージョンにします。
2. テストを実行し、native imageで実行ファイルを作ります。
3. 実行ファイルを`--randomize`で1回起動して、動くかを確かめます。
4. 実行ファイルと`dll`をzipにします。
5. GitHub Releaseを作り、`sss-wallpaper-1.2.0.zip`を添付します。

バージョンは、zipの名前とReleaseの題にだけ入ります。実行ファイルには入りません。

タグは`v1.2.0`のように、`v`と数字とドットだけで書きます。形式が違うタグをpushすると、ワークフローは最初のステップで失敗します。

`main`へpushしたときも、CIはテストの後に実行ファイルを作り、`--randomize`で1回起動します。できた実行ファイルは、GitHub Actionsの実行結果の「Artifacts」から7日間ダウンロードできます。
