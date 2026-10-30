# ネイティブビルドの調査

2026-10-31に調べた結果である。GraalVMのnative imageで、単体の`.exe`を作る方法を調べた。今のCIのjpackageは、JDKを同梱したアプリのフォルダを作るもので、これとは別物である。

この文書は、調べた時点の記録である。版や対応状況は変わるので、試す前に出典を確かめ直すこと。ファイルの変更はまだしていない。

## 調べたときのアプリの構成

- Windows専用のJavaFXアプリ。Maven、Java 25（Temurin）、JavaFX 27.0.1、AtlantaFX 3.0.0、javafx-builder-api
- WindowsのCOMインタフェース`IDesktopWallpaper`を、FFM（`java.lang.foreign`のdowncall）で呼ぶ（`DesktopWallpaperService`）
- 画像の合成に、`java.awt`の`BufferedImage`と`Graphics2D`、`javax.imageio`の`ImageIO`を使う（`FitImageComposer`）
- `module-info`はなく、`Launcher`クラスからクラスパスで起動する。1つの経路で`Platform.startup`を使う
- CIは、windows-latestでjpackageのアプリのフォルダを作り、アップロードする。`release.yml`は、`v*`のタグでzipを作る

## 結論

実現できる見込みは高い。ただし、Gluonの道具は使わない。最新の構成は、**Oracle GraalVM 25系とHebiRoboticsの`jfx-static-feature`**である。

いちばん大きい障害は、JavaFXを`27.0.1`から`27`へ下げる必要があることである。

## 1. 道具とバージョン（確認済み）

### Gluon（勧めない）

- `gluonfx-maven-plugin`の最新は1.0.29である（2026-06-18、Maven Central）。
- Substrateの最新は0.0.69である（2026-06-18）。既定の`DEFAULT_JAVAFX_STATIC_SDK_VERSION`は`21-ea+11.3`のままである。
- Gluon版GraalVMの最新は`gluon-23+25.1-dev`で、2024-09のものである。
- 公式ドキュメントのWindowsの手順は、Java 17版のGraalVM 22.1とVisual Studio 2019のままである。
- まとめると、Java 25とJavaFX 27に対応するという記述は見つからなかった。

### StaticFX（勧める）

- 組み合わせは`us.hebi.graalvm:jfx-static-libs`と`jfx-static-feature`である。
- 最新は`27-2`と`1.1.1`で、どちらも2026-10の公開である。
- READMEの対応表では、この組み合わせはOracle GraalVM 25.4（25.4.4.1.1）向けである。
- Windows amd64の静的ライブラリが、リリースに含まれている。
- 制約が1つある。静的ライブラリは、クラスパス上のJavaFXのjarと同じコミットでなければならない。ビルドのときに検査もされる。READMEには「`27`は`27.0.1`と組み合わせられない」と明記されている。
- 「internal」と名の付いた、非公式のビルドである。

### Liberica NIK

- 25.0.4-1（2026-07）が出ている。JavaFX入りのNIK-fullもある。
- ただし、同梱されるJavaFXは独自のものである。Maven上の27.0.1とは合わない。
- 確かめられたJavaFXの修正は、Linux向けだけだった。

## 2. GitHub Actions

- `graalvm/setup-graalvm@v1`の入力は、`distribution: graalvm`、`version: '25.4'`、`cache: maven`である（READMEによる。日付なし）。
- GraalVM 25のドキュメントには、Visual Studioがあれば、`native-image`がビルド環境を自動で設定すると書かれている。
- ここからは推測である。windows-latestにはVisual Studioが入っているので、`ilammy/msvc-dev-cmd`は通常いらない。失敗したときの保険として足せば十分である。
- ビルド時間とメモリの公式の数値は、見つからなかった。推測では、JavaFXアプリで5〜15分、ヒープは数GBかかる。ランナーの16GBで足りる見込みである。

ワークフローの例を次に示す。

```yaml
native:
  runs-on: windows-latest
  steps:
    - run: git config --global core.autocrlf input
    - uses: actions/checkout@v7
    - uses: graalvm/setup-graalvm@v1
      with:
        distribution: graalvm
        version: '25.4'
        cache: maven
        github-token: ${{ secrets.GITHUB_TOKEN }}
        native-image-job-reports: 'true'
    - run: mvn -B -Pnative package -DskipTests
    - uses: actions/upload-artifact@v7
      with:
        name: sss-wallpaper-native
        path: target/*.exe
```

`native`プロファイルの中身は、次のとおりである。

- `org.graalvm.buildtools:native-maven-plugin`に、`mainClass`として`Launcher`を指定する。
- buildArgに`--enable-native-access=ALL-UNNAMED`を足す。
- 依存に`jfx-static-feature`と`jfx-static-libs`を足す。
- 今の`package`は、jpackageも動かす。プロファイルで分けるか、jpackageを外す必要がある。

## 3. このアプリに特有の危険

### FFM（確認済み）

- GraalVM 25からは、FFMが既定で有効である。Windows/x64のdowncallにも対応している。
- `SymbolLookup.libraryLookup`も使える。
- `reachability-metadata.json`の`foreign.downcalls`に、使うシグネチャをすべて登録する。

### FFM（推測）

- `DesktopWallpaperService`は、COMのvtableから関数ポインタを取り、その場で`downcallHandle`を作る。
- 登録の単位はシグネチャなので、この作り方でも問題ないはずである。
- 登録が要る`FunctionDescriptor`は、ソースから見て7種類ほどである。

### FFMの注意

- downcallのスタブは、ビルド時の静的初期化では作れない。
- GraalVM 25では、クラスは既定で実行時に初期化される。そのため、`static final`のハンドルはそのままで大丈夫な見込みである。

### AWTとImageIO（`FitImageComposer`）

- いちばん不確かな点である。
- StaticFXのREADMEには「GraalVM 25は静的なAWTライブラリを同梱するが、AWTのメタデータはない。自分で書く」とある。
- oracle/graal #3084は古い記録で、Windowsには対応していないと書かれている。新しい状況は確かめられなかった。
- 推測では、画面に出さない`BufferedImage`とPNGの読み書きなら、動く可能性がある。ただし、トレーシングエージェントでメタデータを取る必要がある。

### JavaFX、AtlantaFX、リソース

- JavaFXのメタデータは、`jfx-static-libs`に含まれる。
- AtlantaFXのCSS、アイコンの`sss-wallpaper.png`、javafx-builderは、自分で登録する。
- READMEには、AtlantaFXのサンプラーをネイティブ化した計測がある。AtlantaFXには実績があると見てよい。

### `Platform.startup`

- READMEで問題とされているのは、macOSだけである。Windowsでは問題がない。

### トレーシングエージェント

- `-agentlib:native-image-agent=config-output-dir=...`を付けて、JVMで起動する。
- 次の3つの経路を、手で通す。
  - 通常のGUI
  - `--randomize`
  - エラーダイアログ
- 推測である。エージェントがFFMのdowncallも記録するかは、確かめていない。足りなければ手で書く。

## 4. 判断と最初の一歩

### 障害

1. JavaFXを`27`へ下げるか、`27.0.1`用の静的ライブラリを待つ必要がある。
2. Windowsで`java.awt`と`ImageIO`が動くかを、確かめられていない。
3. Oracle GraalVMのライセンスはGFTCである。配布するなら、条件を確かめる必要がある。

### 最初の一歩

CIより先に、手元で動くかを確かめる。

1. 手元のWindowsに、Oracle GraalVM 25.4を入れる。
2. 別のブランチで、JavaFXを`27`にし、`native`プロファイルを足す。
3. まずは、メタデータなしでビルドし、ウィンドウが出るかを確かめる。
4. 次に、トレーシングエージェントでメタデータを集め、`--randomize`の経路を確かめる。FFMとImageIOが動くかが、ここで分かる。

手元で動いてから、CIに載せる。

## 出典

- StaticFXのREADME（対応表は2026-10の時点）: https://github.com/HebiRobotics/jfx-static-feature
- jfx-static-libs 27-2（2026-10-17）: https://github.com/ennerf/jfx/releases
- GraalVM 25のFFM: https://www.graalvm.org/jdk25/reference-manual/native-image/native-code-interoperability/ffm-api
- setup-graalvm: https://github.com/graalvm/setup-graalvm
- GluonFXとSubstrate: https://github.com/gluonhq/gluonfx-maven-plugin/releases 、 https://github.com/gluonhq/substrate （`Constants.java`）
- Gluonのドキュメント（古い内容）: https://docs.gluonhq.com/
- Gluon版GraalVM（2024-09）: https://github.com/gluonhq/graal/releases
- Liberica NIK: https://docs.bell-sw.com/liberica-nik/latest/general/release-notes
- WindowsでのAWT（古い記録）: https://github.com/oracle/graal/issues/3084
