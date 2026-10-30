# ネイティブビルドの調査

2026-10-31に調べた結果である。GraalVMのnative imageで、単体の`.exe`を作る方法を調べた。今のCIのjpackageは、JDKを同梱したアプリのフォルダを作るもので、これとは別物である。

この文書は、調べた時点の記録である。版や対応状況は変わるので、試す前に出典を確かめ直すこと。同じ日に手元で試した結果は、「5. 手元で試した結果」に書いた。

## 調べたときのアプリの構成

- Windows専用のJavaFXアプリ。Maven、Java 25（Temurin）、JavaFX 27.0.1、AtlantaFX 3.0.0、javafx-builder-api
- WindowsのCOMインタフェース`IDesktopWallpaper`を、FFM（`java.lang.foreign`のdowncall）で呼ぶ（`DesktopWallpaperService`）
- 画像の合成に、`java.awt`の`BufferedImage`と`Graphics2D`、`javax.imageio`の`ImageIO`を使う（`FitImageComposer`）
- `module-info`はなく、`Launcher`クラスからクラスパスで起動する。1つの経路で`Platform.startup`を使う
- CIは、windows-latestでjpackageのアプリのフォルダを作り、アップロードする。`release.yml`は、`v*`のタグでzipを作る

## 結論

実現できる見込みは高い。Gluonの道具（GluonFX、Gluon版GraalVM）は採用しない。最新の構成は、**Oracle GraalVM 25系とHebiRoboticsの`jfx-static-feature`**である。

いちばん大きい障害は、JavaFXを`27.0.1`から`27`へ下げる必要があることである。

## 1. 道具とバージョン（確認済み）

### Gluon（採用しない）

Gluonの道具は採用しない。調べた内容は、判断の材料として残す。

- `gluonfx-maven-plugin`の最新は1.0.29である（2026-06-18、Maven Central）。
- Substrateの最新は0.0.69である（2026-06-18）。既定の`DEFAULT_JAVAFX_STATIC_SDK_VERSION`は`21-ea+11.3`のままである。
- Gluon版GraalVMの最新は`gluon-23+25.1-dev`で、2024-09のものである。
- 公式ドキュメントのWindowsの手順は、Java 17版のGraalVM 22.1とVisual Studio 2019のままである。
- まとめると、Java 25とJavaFX 27に対応するという記述は見つからなかった。

### StaticFX（採用候補）

仕組みは、次の「StaticFXの仕組み」節で説明する。

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

## StaticFXの仕組み

StaticFX（`jfx-static-feature`）は、2つの部品と、アプリ側で用意するメタデータでできている。この節は、READMEを読んで確かめた内容である。READMEに書かれていない説明は、その旨を書く。

### 前提: なぜ特別な仕組みが要るのか

この小節はREADMEの内容ではなく、背景の説明である。

- 普通のJVMで動くJavaFXは、jarの中のDLLを取り出し、`System.loadLibrary`で読み込む。
  - 例: `glass.dll`（ウィンドウ）、`prism_d3d.dll`（描画）、`javafx_font.dll`（フォント）
  - このプロジェクトのテストで、`~/.openjfx/cache`への取り出しが衝突した件が、まさにこの仕組みである。
- native imageは、ビルドのときに、到達できるコードだけを1つの`.exe`にまとめる。そのため、実行時の動きをビルドのときに知っておく必要がある。
  - リフレクションで使うクラス
  - JNIで呼ぶネイティブメソッド
  - 読み込むリソース
- JavaFXは、この3つをどれも大量に使う。そこで、JavaFXのネイティブコードを静的ライブラリにして`.exe`に直接リンクし、必要なメタデータを揃える仕組みが要る。

### 部品1: `jfx-static-libs`（材料）

- JavaFXの版ごとに1つのjarである。中身は2つある。
  - 全プラットフォーム分の**静的ライブラリ**（`.lib`や`.a`）
  - それに合う**メタデータ**。JavaFX内部のリフレクションとJNIで、1,000件近くある。
- 作者のフォーク（ennerf/jfx）でビルドしている。公式のリリースのタグに`@Reachable`という注釈を足し、コンパイル時にメタデータを生成する。製品のコードは変えていない、と書かれている。
- webとmediaのモジュールは、静的ライブラリにできない。この2つは、普通のDLLを`.exe`の隣に置いて使う。

### 部品2: `jfx-static-feature`（組み立て役）

GraalVMの`Feature`インタフェースの実装である。`Feature`は、native imageのビルドの途中に割り込んで、処理を足すための仕組みである。ビルドのときに、次のことをする。

- 今のプラットフォーム用の静的ライブラリを取り出し、「組み込みのJNIライブラリ」として登録する。これで、ネイティブメソッドがリンクの時点で確かめられる。
- OSのライブラリのリンクの指定を足す。LinuxのGTKや、macOSのCocoaなどである。
- `Application`の子クラスの、引数のないコンストラクタを、リフレクション用に登録する。`Application.launch`は、リフレクションでアプリのクラスを作るためである。
- JavaFXのコードの一部を、置き換え（substitution）で差し替える。例は次の2つである。
  - `System.loadLibrary`では、静的ライブラリを読み込めない。そこで、ライブラリの初期化の関数を直接呼ぶように差し替える。
  - native imageは、JNIのメソッドを短い名前で解決する。そのため、オーバーロードされたネイティブメソッドを区別できない。これも差し替えで直す。

### 版を合わせる理由

- 静的ライブラリは、クラスパス上のJavaFXのjarと、**まったく同じコミット**から作られている必要がある。
- ここからは推測である。Java側のコードとネイティブ側のコードは、JNIの名前とシグネチャで結び付いている。コミットがずれると、この結び付きがずれる可能性がある。
- そのため、ビルドのときに版の一致が検査される。`27`のライブラリは、`27.0.1`のjarとは組み合わせられない。
- 修正版の番号は、`${javafx.version}-${patch}`の形である。例えば`27-2`は、JavaFX `27`に対する2回目の修正である。

### Mavenでの使い方（READMEの手順）

1. GraalVM Build Toolsの`native-maven-plugin`を足し、`<mainClass>`を指定する。
2. 依存に、次の2つを足す。
   - `us.hebi.graalvm:jfx-static-feature:1.1.1`
   - `us.hebi.graalvm:jfx-static-libs:${javafx.version}`（runtimeスコープ）
3. `mvn -Pnative package`でビルドする。
4. 含まれるメタデータは、JavaFXの分だけである。アプリ自身のリフレクションは、自分で登録する。

### このアプリに関係しそうな点

- Windowsでは、既定でコンソールのないGUIアプリとして作られるようである。コンソールを残すには、`-Djfx.static.gui=false`を指定する。
  - これは、ウィンドウなしの起動のエラーをダイアログで知らせる判断（PR #12）と合っている。native imageでも、標準エラーは見えないためである。
- メタデータの対象は、JavaFXだけである。次のものは、自分で登録する必要がある。
  - FFMのdowncall
  - AWTとImageIO
  - AtlantaFXのCSS
  - アプリのアイコン

### Gluonとの違い

READMEには、Gluonとの比較が書かれていない。この小節は知識による説明で、確かめてはいない。

- Gluonは、JavaFX専用に調整したGraalVMと、Gluonが作った静的SDKを使う。
- StaticFXは、普通のOracle GraalVMと、公式のJavaFXのタグからビルドした静的ライブラリを使う。

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

## 5. 手元で試した結果

2026-10-31に、手元のWindows 11で試した。GUI、`--randomize`、エラーダイアログのどれも、ネイティブのexeで動いた。

### 使った道具

- GraalVM CE 25.4（`graal-25.4.4.1.1`、JDK 25.0.4）を使った。Oracle GraalVMではない。
  - SDKMANでOracle GraalVM 25.4を入れようとしたが、Windows用のファイルが404で取れなかった。
  - Oracleの`25/latest`は25.0.4で、25.4ではなかった。
  - CEはGitHubのリリースから取り、sha256を確かめた。
- CEのライセンスはGPLv2とClasspath Exceptionである。GFTCの条件を確かめる必要は、CEを使う限りなくなる。
- `JAVA_HOME`にCEを指定し、`mvn -Pnative clean package -DskipTests`でビルドした。既定のJDKは、Temurinのままでよい。
- C言語のコンパイラは、Visual Studio 2022の`cl.exe`（19.44）が自動で見つかった。

### `native`プロファイル

`pom.xml`に`native`プロファイルを足した。

- JavaFXを`27`にし、`jfx-static-feature` 1.1.1と`jfx-static-libs` 27-2を足す。
- `native-maven-plugin` 1.1.18で、`package`の段階にexeを作る。
- `--enable-native-access=ALL-UNNAMED`を渡す。
- jpackage用の処理（依存のコピー、jpackage、読み取り専用の解除）は、このプロファイルでは止める。

### メタデータなしでのビルド

- ビルドは通り、44MBのexeができた。
- 実行すると、次の2つで失敗した。
  - `DesktopWallpaperService`の初期化で、`MissingForeignRegistrationError`が出た。FFMのdowncallのシグネチャが登録されていないためである。
  - AtlantaFXのCSS（`/atlantafx/base/theme/primer-light.css`）が見つからなかった。

### トレーシングエージェント

- GraalVMのJVMで、`-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/com.sosuisha/sss-wallpaper`を付けて起動した。
  - `config-merge-dir`にしたのは、3回の実行の結果を1つのファイルにまとめるためである。
- クラスパスは、`target/classes`と、JavaFX、javafx-builder、AtlantaFX、JSpecifyのjarにした。
  - `dependency:build-classpath`の結果には、テスト用のjarも混ざっていた。それらは手で除いた。
- 次の3つの経路を通した。
  - 通常のGUI。ドロップ、壁紙の設定、「場所を開く」を手で操作した。
  - `--randomize`
  - エラーダイアログ。`-Dsss.settings.file`にフォルダを指定し、設定の読み込みを失敗させた。
- 結果は、161KBの`reachability-metadata.json`になった。
- エージェントは、FFMのdowncallも`foreign.downcalls`に記録した。「3. このアプリに特有の危険」の推測は当たっていた。手で書く必要はなかった。

### メタデータありでのビルド

- ビルドは47秒で通り、52MBのexeができた。
- `--randomize`は、終了コード0で終わり、壁紙が変わった。
- GUIでは、次のすべてが動いた。
  - AtlantaFXのテーマ
  - ウィンドウのアイコン
  - 画像のドロップと壁紙の設定。ここで、AWTと`ImageIO`も動いた。
  - 「場所を開く」
- 「4. 判断と最初の一歩」の障害2（`java.awt`と`ImageIO`）は、Windowsでは問題にならなかった。
- 実行すると、`Unsupported JavaFX configuration: classes were loaded from 'unnamed module ...'`という警告が出る。JavaFXをクラスパスから読むと出る警告で、JVMで動かしたときも同じである。

### 配布するときの注意

exeの隣に、次のDLLが出力された。AWTと`ImageIO`を使うためである。

`awt.dll` `fontmanager.dll` `freetype.dll` `java.dll` `javaaccessbridge.dll` `javajpeg.dll` `jawt.dll` `jvm.dll` `lcms.dll`

exe単体では動かない見込みである。配布するときは、これらのDLLも一緒に置く。

### トレーシングをやり直す場面

メタデータは、リポジトリにコミットしておく。`native-image`は、ビルドのたびに`META-INF/native-image/`の下から自動で読む。そのため、毎回トレーシングする必要はない。

やり直すのは、ネイティブイメージから見える動的な使い方が増えたときである。

- FFMで、これまでにない形の関数を呼ぶ。引数や戻り値の型の組み合わせが新しい場合である。
- CSSや画像など、新しいリソースを読む。
- 新しいJavaFXのコントロールを使う。CSSの処理などで、リフレクションが使われることがある。
- JavaFXやAtlantaFXの版を上げる。

やり直しを忘れても、ビルドは通る。足りないことが分かるのは、exeを実行したときである。そこで、CIでexeを`--randomize`で1回起動する確認を入れるとよい。ただし、CIのマシンで壁紙のAPIが動くかは、まだ確かめていない。

### 次の一歩

CIに載せる。「2. GitHub Actions」の案を、CEで動くように直して試す。

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
