# ADR 2: Native Image Only Distribution

## Status

Accepted

## Context

sss-wallpaperは、Windowsの壁紙を変えるJavaFXアプリである。ふだんはウィンドウを開いて使う。`--randomize`を付けて起動すると、ウィンドウを出さずに壁紙をランダムに変えて終わる。READMEでは、この起動をWindowsのログオン時に実行する使い方を勧めている。

これまでは、jpackageでアプリのイメージを作って配っていた。アプリのイメージは、JDKのランタイムを同梱したフォルダである。CIはmainへのpushでこれを作り、`release.yml`はタグでzipにしてGitHub Releaseに置く。

この配り方では、起動のたびにJVMが立ち上がる。ウィンドウを開くときも、起動の待ち時間は短いほうがよい。このアプリは、壁紙を変えたいと思ったときにさっと開いて、すぐ閉じる使い方をするためである。`--randomize`では、この問題がさらに目立つ。仕事は壁紙を1回変えるだけなので、JVMの起動にかかる時間が、仕事そのものに比べて大きくなる。ログオン時には、ほかのアプリも一斉に起動する。この待ち時間は測っていない。

GraalVMのnative imageを使えば、Javaのアプリを、JVMなしで動く実行ファイルにできる。JVMの起動がなくなるので、起動は速くなる見込みである。ただし、JavaFXのアプリをnative imageにするには、JavaFXの静的なライブラリが要る。その手段は、Gluonの道具か、HebiRoboticsのStaticFX（`jfx-static-feature`と`jfx-static-libs`）である。

StaticFXには、JavaFXの版ごとに静的なライブラリが用意される。2026-10-31の時点では、最新の`27-2`はJavaFX `27`用である。このアプリは`27.0.1`を使っていた。

native imageでは、リフレクション、リソース、FFMのdowncallを、ビルド時に知らせる必要がある。知らせ忘れても、ビルドは通る。失敗するのは、exeを実行したときである。

この情報は、機能が変わるたびに作り直す必要がある。開発の途中でnative imageにすると、その手間が何度もかかる。2026-10-31の時点で、このアプリの機能はほとんど固まり、完成している。今後、作り直す機会は少ない見込みである。

2026-10-31に、GraalVM CE 25.4とStaticFXで試した。手元のWindowsでも、GitHub Actionsでも、exeを作れた。手元では、GUI、`--randomize`、エラーダイアログのどれも動いた。調査と試した結果は、[ネイティブビルドの調査](../native-image-research.md)にある。

## Decision

配布物を、GraalVMのnative imageで作るexeだけにする。jpackageのアプリのイメージは、作るのも配るのもやめる。

ビルドには、GraalVM CE 25.4を使う。Oracle GraalVMは使わない。Oracle GraalVMのライセンスはGFTCで、配布する前に条件を確かめる必要があるためである。CEのライセンスは、GPLv2とClasspath Exceptionである。

JavaFXの静的なライブラリには、StaticFXを使う。`jfx-static-feature`は1.1.1、`jfx-static-libs`は`27-2`とする。`jfx-static-libs`の版に合わせて、JavaFXをプロジェクト全体で`27`にする。テストする版と配る版を、同じにするためである。

ネイティブビルドは、Mavenの`native`プロファイルに置く。`mvn -Pnative package`で、`native-maven-plugin`がexeを作る。ふだんの`mvn package`では、exeを作らない。テストは、プロファイルなしのまま、TemurinのJDKで動かす。GraalVMが要るのは、exeを作るときだけである。

native imageに知らせる情報は、`src/main/resources/META-INF/native-image/com.sosuisha/sss-wallpaper/reachability-metadata.json`に置き、リポジトリで管理する。このファイルは、トレーシングエージェントで作る。GUI、`--randomize`、それぞれのエラーダイアログの4つの経路を通して記録する。FFMの新しいシグネチャ、新しいリソース、新しいJavaFXのコントロールを使うとき、またはJavaFXやAtlantaFXの版を上げるときに、作り直す。

CIでは、jpackageの代わりにexeを作る。`ci.yml`の`package`ジョブは、mainへのpushでexeを作り、`--randomize`で1回起動する。メタデータの不足は実行時にしか分からないので、ここで早く見つける。`release.yml`は、タグでexeを作り、zipにしてGitHub Releaseに置く。

zipには、exeと、その隣に出力されるDLLを入れる。DLLは、AWTと`ImageIO`のために要る。版は、zipの名前とReleaseの題にだけ入れる。exeには版を埋め込まない。

exeのアイコンは、Windowsのリソースとして埋める。`src/windows/sss-wallpaper.rc`をコンパイルした`.res`を、`-H:NativeLinkerOption`でリンカーに渡す。`.res`はリポジトリで管理する。コンパイルに使う`rc.exe`は、ふつうのシェルにもCIにもPATHが通っていないためである。

## Alternatives

### Alternative 1: jpackageのアプリのイメージを配り続ける

今の配り方を続ける。メタデータを管理する必要がなく、JavaFXの版も自由に選べる。しかし、起動のたびにJVMが立ち上がるので、起動の速さという目的を果たせない。

### Alternative 2: jpackageとネイティブの両方を配る

利用者が、どちらかを選べる。ネイティブで問題が起きたときの逃げ道にもなる。しかし、CIの時間と、確かめる手間が2倍になる。1人で開発するアプリに、2つの配り方は重い。

### Alternative 3: Gluonの道具（GluonFX、Gluon版GraalVM）を使う

JavaFXのnative imageでは、いちばん実績のある道具である。しかし、対応する版が古いまま更新されていない。Gluon版GraalVMの最新は`gluon-23+25.1-dev`で、2024-09のものである。Substrateの既定のJavaFXの静的なSDKは、`21-ea+11.3`のままである。公式ドキュメントのWindowsの手順も、Java 17版のGraalVM 22.1のままである。Java 25とJavaFX 27に対応するという記述は、見つからなかった。

### Alternative 4: Oracle GraalVMを使う

GraalVMの機能がすべて使え、性能も高いとされる。しかし、ライセンスがGFTCで、配布する前に条件を確かめる必要がある。CEで動いたので、確かめる手間をかける理由がない。

### Alternative 5: JavaFXを`27.0.1`のまま、静的なライブラリを待つ

JavaFXの版を下げずに済む。しかし、`27.0.1`用の`jfx-static-libs`がいつ出るかは分からない。`27`と`27.0.1`の差は小さいので、待つ理由は弱い。

### Alternative 6: Liberica NIKを使う

JavaFX入りのNIK-fullがあり、25.0.4-1（2026-07）が出ている。しかし、同梱されるJavaFXは独自のビルドで、Maven上のJavaFXとは合わない。テストする版と配る版を、同じにできない。

## Confidence Level

確信度は中程度である。

手元のWindowsとGitHub Actionsで、exeを作って動かせた。GUI、`--randomize`、エラーダイアログの経路も、手元で確かめた。技術的に実現できることには、確信がある。

一方で、次の点が不確かである。

StaticFXは、「internal」と名の付いた非公式のビルドである。HebiRoboticsが更新をやめれば、JavaFXの版を上げられなくなる。また、READMEの対応表では、StaticFXはOracle GraalVM向けとされている。CEで動いたのは、試した結果であって、保証されたものではない。

起動が速くなるという目的も、まだ測っていない。見込みは高いが、確かめてはいない。

次のときは、この決定を見直す。

- StaticFXの更新が止まり、JavaFXの版を上げられなくなったとき。
- GraalVM CEで、StaticFXが動かなくなったとき。
- JavaFXが、公式にnative imageへ対応したとき。そのときは、StaticFXに頼る理由がなくなる。
- 機能を大きく足すことになり、メタデータの作り直しの手間が、起動の速さに見合わなくなったとき。

## Consequences

### 良い結果

起動のたびにJVMが立ち上がらなくなる。GUIも`--randomize`も、起動が速くなる見込みである。

配布物から、JDKのランタイムがなくなる。zipの中身は、exeと9つのDLLだけになる。

### 悪い結果

メタデータを管理する手間が増える。動的な使い方が増えるたびに、トレーシングエージェントで作り直す必要がある。ただし、機能はほとんど固まっているので、作り直す機会は少ない見込みである。

作り直しを忘れても、ビルドもテストも通る。テストはJVMで動くためである。不足に気づくのは、exeを実行したときになる。作り直す機会が少ないぶん、手順を忘れやすい。そのため、手順はREADMEの「メタデータ」に残す。

CIでは、exeを`--randomize`で1回起動して、不足を早く見つける。しかし、CIには画像のフォルダが登録されていない。そのため、確かめられるのは、設定の読み込みとモニターの一覧を取る処理までである。画像の合成、壁紙の設定、GUIの不足は、CIでは見つからない。

JavaFXの版は、StaticFXが出す静的なライブラリに縛られる。JavaFXの新しい版が出ても、対応する`jfx-static-libs`が出るまで上げられない。

exeを作るには、GraalVM CE 25.4とVisual Studioが要る。手元でexeを作るときは、`JAVA_HOME`をGraalVMに切り替える必要がある。

exeには版の情報がない。版は、zipの名前とReleaseの題でしか分からない。

アイコンの`.res`は生成物だが、リポジトリに置く。アイコンを変えたときは、手で作り直す必要がある。

### 中立的な結果

`pom.xml`から、jpackageのための設定を消す。依存のコピー、jpackage、読み取り専用の解除の3つである。`app.version`も使わなくなるので消す。

試しに作った`native.yml`は、`ci.yml`の`package`ジョブに統合する。

READMEの「実行ファイルを作る」と「リリース」の節を、ネイティブビルドの手順に書き直す。

実行すると、`Unsupported JavaFX configuration`の警告が出る。JavaFXをクラスパスから読むと出る警告で、jpackageのときも同じだった。
