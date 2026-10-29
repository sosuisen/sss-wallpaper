# ADR 1: IDesktopWallpaper COM via FFM API

## Status

Accepted

## Context

sss-wallpaperは、Windowsのデスクトップ壁紙を変えるJavaFXアプリである。このアプリは、モニターごとに別の壁紙を設定する。そのため、Windowsに接続された各モニターを列挙し、その位置と大きさを得る必要がある。また、各モニターの現在の壁紙を読み戻して、サムネイルに表示する必要もある。

Javaの標準ライブラリとJavaFXには、OSの壁紙を変える機能がない。そのため、WindowsのネイティブAPIを呼ぶ必要がある。

Windowsで壁紙を変えるAPIは主に2つある。1つは`SystemParametersInfo`の`SPI_SETDESKWALLPAPER`である。もう1つはCOMの`IDesktopWallpaper`である。前者は、全モニターに同じ壁紙を1枚だけ設定する。後者は、モニターのデバイスパスを指定して壁紙を設定・取得できる。モニターの列挙と矩形の取得もできる。

JavaからネイティブAPIを呼ぶ方法も複数ある。JNIは、C言語のコードとビルドが必要になる。JNAは、外部ライブラリへの依存が増える。このプロジェクトはJava 25を使う。Java 22で正式になったFFM APIを、追加の依存なしで使える。

## Decision

壁紙の設定には、WindowsのCOMインターフェース`IDesktopWallpaper`を採用する。モニターの列挙には`GetMonitorDevicePathCount`と`GetMonitorDevicePathAt`を使う。位置と大きさは`GetMonitorRECT`で得る。壁紙の設定は`SetWallpaper`で、現在の壁紙の取得は`GetWallpaper`で行う。

COMの呼び出しには、FFM API（`java.lang.foreign`）を採用する。`ole32`の`CoInitializeEx`と`CoCreateInstance`で`IDesktopWallpaper`のインスタンスを作る。メソッドは、vtableのインデックスから関数ポインタを取り出して呼ぶ。この処理は`DesktopWallpaperService`に閉じ込める。ドメイン層は`WallpaperService`インターフェースだけに依存する。

失敗したCOMの呼び出しは、HRESULTが負のとき`WallpaperException`に変換する。FFM APIのネイティブアクセスを許可するために、実行時に`--enable-native-access=ALL-UNNAMED`を指定する。

### 実装に必要なWindows APIの情報

`DesktopWallpaperService`は、次の情報を定数として持つ。

| 項目 | 値 | 定義している場所 |
|---|---|---|
| `CLSID_DesktopWallpaper` | `C2CF3110-460E-4FC1-B9D0-8A1C0C9CC4BD` | `um\ShObjIdl_core.h` |
| `IID_IDesktopWallpaper` | `B92B56A9-8B55-4E14-9A89-0199BBB6F93B` | `um\ShObjIdl_core.h` |
| `COINIT_APARTMENTTHREADED` | `0x2` | `um\objbase.h` |
| `CLSCTX_ALL` | `0x17` | `um\combaseapi.h`、`shared\wtypesbase.h` |

COMのインターフェースは、メソッドの一覧である。Javaの`interface`とほぼ同じ考え方である。インターフェースは中身を持たない。実装はCOMのクラスが持つ。COMでは、クラスもインターフェースも、名前ではなくGUIDで区別する。

| COM | 意味 | Javaでたとえると | このアプリでの値 |
|---|---|---|---|
| CLSID | クラスのGUID | `DesktopWallpaperService`のようなクラス | `CLSID_DesktopWallpaper` |
| IID | インターフェースのGUID | `WallpaperService`のようなインターフェース | `IID_IDesktopWallpaper` |

`CoCreateInstance`は、COMのオブジェクトを作る関数である。CLSIDとIIDの2つを渡す。これは「`CLSID_DesktopWallpaper`のクラスでオブジェクトを作り、`IDesktopWallpaper`として使えるポインタを返す」という依頼である。Javaでたとえると、次のコードに近い。

```java
WallpaperService service = new DesktopWallpaperService();
```

`new`の後ろがCLSIDに当たる。左側の型`WallpaperService`がIIDに当たる。

IIDを渡す理由は、1つのCOMクラスが複数のインターフェースを実装できるからである。Javaのクラスが複数の`interface`を実装できるのと同じである。そのため、どのインターフェースとして使いたいかを、呼ぶ側が指定する。

COMは、指定されたインターフェースの形でポインタを返す。このポインタの先には、そのインターフェースのvtable（メソッドの関数ポインタの表）がある。`IDesktopWallpaper`を指定したので、vtableの並びは`IDesktopWallpaperVtbl`のとおりになる。オブジェクトが指定のインターフェースを実装していなければ、`CoCreateInstance`は失敗する。そのときは`E_NOINTERFACE`（`0x80004002`）という負のHRESULTが返る。

`CLSCTX_ALL`は、`CLSCTX_INPROC_SERVER`（`0x1`）、`CLSCTX_INPROC_HANDLER`（`0x2`）、`CLSCTX_LOCAL_SERVER`（`0x4`）、`CLSCTX_REMOTE_SERVER`（`0x10`）の論理和である。

`IDesktopWallpaper`のメソッドは、vtableの順に次のとおりである。最初の3つは、すべてのCOMインターフェースが持つ`IUnknown`のメソッドである。

| インデックス | メソッド | 引数（`This`を除く） |
|---|---|---|
| 0 | `QueryInterface` | `REFIID`, `void**` |
| 1 | `AddRef` | なし |
| 2 | `Release` | なし |
| 3 | `SetWallpaper` | `LPCWSTR monitorID`, `LPCWSTR wallpaper` |
| 4 | `GetWallpaper` | `LPCWSTR monitorID`, `LPWSTR* wallpaper` |
| 5 | `GetMonitorDevicePathAt` | `UINT monitorIndex`, `LPWSTR* monitorID` |
| 6 | `GetMonitorDevicePathCount` | `UINT* count` |
| 7 | `GetMonitorRECT` | `LPCWSTR monitorID`, `RECT* displayRect` |

FFM APIでは、ネイティブの関数の引数と戻り値の型を`FunctionDescriptor`で宣言する。Cの型には、`java.lang.foreign.ValueLayout`の定数を当てはめる。`ADDRESS`はポインタを表す定数である。64bitのWindowsでは8バイトになる。

| Cの型 | FFM APIの定数 | 大きさ |
|---|---|---|
| ポインタ（`void*`、`LPCWSTR`、`RECT*`など） | `ADDRESS` | 8バイト（64bitのWindows） |
| `HRESULT`、`UINT`、`LONG`、`int` | `JAVA_INT` | 4バイト |
| `WORD`、`short` | `JAVA_SHORT` | 2バイト |
| `BYTE` | `JAVA_BYTE` | 1バイト |

FFM APIで呼ぶときは、次の対応で型を当てはめる。

- COMのメソッドは、第1引数に`This`を取る。`This`は、呼び出し先のCOMオブジェクトを指すポインタである。Javaの`this`と同じ役割である。ドキュメントの引数一覧には`This`が載っていない。そのため、`FunctionDescriptor`では、ドキュメントの引数の前に`ADDRESS`を1つ足す。
- 戻り値の`HRESULT`は32bitの整数である。`JAVA_INT`で受ける。負の値は失敗を表す。
- `LPCWSTR`と`LPWSTR`はUTF-16LEの文字列へのポインタである。終端は`\0`である。
- `GetWallpaper`などが返す`LPWSTR`は、COMが確保したメモリである。読んだ後に`CoTaskMemFree`で解放する。
- `RECT`は、`left`、`top`、`right`、`bottom`の4つの`LONG`（32bit）である。幅は`right - left`で求める。
- GUIDの構造は、`DWORD Data1`、`WORD Data2`、`WORD Data3`、`BYTE Data4[8]`である。前の3つはリトルエンディアンで並ぶ。そのため、`UUID`の16バイトをそのまま書き込むことはできない。フィールドごとに書き込む。

### 情報の調べ方

**Windows SDKのヘッダーを読む。** 一番確かな情報源である。Windows SDKを入れると、`C:\Program Files (x86)\Windows Kits\10\Include\<バージョン>\`にヘッダーが置かれる。`IDesktopWallpaper`は`um\ShObjIdl_core.h`にある。このファイルで次の文字列を探す。

- `MIDL_INTERFACE("`：直後の文字列がIIDである。
- `CLSID_DesktopWallpaper`：近くの`DECLSPEC_UUID("`の文字列がCLSIDである。
- `IDesktopWallpaperVtbl`：C言語向けのvtableの構造体である。メンバーの並びが、そのままvtableのインデックスになる。引数の型もここで分かる。

定数は、`COINIT_APARTMENTTHREADED`なら`objbase.h`、`CLSCTX_*`なら`wtypesbase.h`と`combaseapi.h`で探す。

**Microsoft Learnのリファレンスを読む。** 各メソッドの意味、引数、戻り値、メモリの解放の責任を調べるのに使う。ただし、リファレンスのメソッド一覧はアルファベット順である。vtableの順ではない。vtableのインデックスは、必ずヘッダーの`IDesktopWallpaperVtbl`で確かめる。

**レジストリで確かめる。** CLSIDとIIDが正しいかは、レジストリで確かめられる。`HKEY_CLASSES_ROOT\CLSID\{C2CF3110-...}`と`HKEY_CLASSES_ROOT\Interface\{B92B56A9-...}`が存在すれば、そのPCにCOMのクラスとインターフェースが登録されている。PowerShellでは次のように確かめる。

```powershell
Test-Path 'Registry::HKEY_CLASSES_ROOT\CLSID\{C2CF3110-460E-4FC1-B9D0-8A1C0C9CC4BD}'
```

**実機のテストで確かめる。** vtableのインデックスや型を間違えても、コンパイラは検出しない。`DesktopWallpaperServiceTest`は、モニターの列挙と壁紙の取得を実機で呼ぶ。`DesktopWallpaperServiceDesktopTest`は、壁紙を設定して読み戻す。値を変えたときは、この2つのテストを実行して確かめる。

## Alternatives

### Alternative 1: SystemParametersInfo（SPI_SETDESKWALLPAPER）

`user32`の`SystemParametersInfoW`で壁紙のパスを設定する方法である。呼び出しは1つの関数だけで済み、実装は簡単である。しかし、全モニターに同じ壁紙を1枚だけ設定する。モニターごとに別の壁紙を設定できない。モニターの列挙もできない。このアプリの中心となる要件を満たさないため、採用しない。

### Alternative 2: SystemParametersInfoとスパン画像の合成

全モニターを覆う1枚の大きな画像をアプリで合成し、「スパン」で設定する方法である。`SystemParametersInfo`だけでモニターごとの見た目を作れる。しかし、モニターの配置と解像度を別のAPIで得る必要がある。配置が変わるたびに画像を作り直す必要もある。DPIやモニターの並びによって、ずれが起きやすい。`IDesktopWallpaper`のほうが直接的で確実なため、採用しない。

### Alternative 3: JNI

JNI（Java Native Interface）は、JDKに昔からある標準の仕組みである。JNIでは、次の手順でネイティブコードを呼ぶ。

- Javaで`native`メソッドを宣言する。
- そのメソッドの中身を、C/C++で書く。
- C/C++をコンパイルしてDLLを作る。
- JavaはそのDLLを読み込んで呼ぶ。

このアプリでJNIを使う場合は、C++で`CoCreateInstance`や`SetWallpaper`を呼ぶ関数を書く。そして、それを自作のDLLにする。COMのヘッダーをそのまま使えるため、型の扱いは安全である。しかし、Visual Studioなどのビルド環境が必要になる。MavenのビルドにネイティブのビルドとDLLの同梱が加わる。保守の負担が大きいため、採用しない。

### Alternative 4: JNA

JNA（Java Native Access）は、JNIの上に作られた外部ライブラリである。JNAでは、次の手順でネイティブコードを呼ぶ。

- C/C++のコードは書かない。
- Javaのインターフェースで、DLLの関数の形を宣言する。
- JNAが実行時に、既存のDLL（`ole32.dll`など）を直接呼ぶ。

自作のDLLは不要である。`jna-platform`にはCOMの補助クラスがあり、コードは短くなる。しかし、JNAのjarへの依存が増える。jpackageで配布するときに、JNAが内部で使うネイティブライブラリの扱いも増える。Java 25ではFFM APIが標準で使えるため、採用しない。

### Alternative 5: PowerShellなどの外部プロセス

PowerShellを起動し、その中からCOMやWin32 APIを呼ぶ方法である。Javaからはプロセスを起動するだけで済む。しかし、呼び出しのたびにプロセスの起動時間がかかる。エラーの受け渡しも文字列頼りになる。実行ポリシーの設定によっては動かない。信頼性が低いため、採用しない。

### FFM API・JNI・JNAの比較

FFM API（Foreign Function & Memory API）は、JNAと同じく、C/C++を書かずに既存のDLLを直接呼ぶ。JNAとの違いは、JDKに標準で入っていることである。外部ライブラリは要らない。

| 方法 | C/C++のコード | 外部ライブラリ | JDK標準 |
|---|---|---|---|
| JNI | 必要 | 不要 | はい |
| JNA | 不要 | 必要 | いいえ |
| FFM API | 不要 | 不要 | はい（Java 22以降） |

まとめると、FFM APIは「JNAの手軽さ」と「JNIの標準性」を両方持っている。これがFFM APIを採用した理由である。

## Confidence Level

この決定の確信度は高い。`IDesktopWallpaper`は、モニターごとの壁紙の設定と取得ができる公式APIである。Windows 8から提供されており、Windows 11でも使える。FFM APIはJava 22で正式になり、Java 25では安定している。

一方で、FFM APIでのCOMの呼び出しは手作業が多い。vtableのインデックス、GUIDのバイトの並び、COMが返す文字列の解放を、自分で正しく扱う必要がある。間違えると、例外ではなくJVMのクラッシュになりうる。この点は、実機の壁紙を設定して読み戻すテストで確認している。

次の状況になったら、この決定を見直す。

- Windowsが`IDesktopWallpaper`を非推奨にした、または動作を変えた。
- 使うCOMのメソッドが大きく増え、手書きのvtable呼び出しの保守が重くなった。そのときは、jextractによるバインディングの生成や、JNAを再検討する。
- Windows以外のOSに対応することになった。

## Consequences

### 良い結果

- モニターごとに別の壁紙を設定できる。各モニターの現在の壁紙も読み戻せる。
- 外部ライブラリへの依存がない。C/C++のコードやネイティブのビルドもない。Mavenのビルドとjpackageでの配布は、Javaだけで完結する。
- ネイティブの呼び出しは`DesktopWallpaperService`に閉じている。ドメイン層とViewModelは`WallpaperService`に依存する。そのため、テストでは偽の実装に差し替えられる。

### 悪い結果

- アプリはWindowsでしか動かない。Windows 8より前のWindowsでも動かない。
- COMの呼び出しを手書きしている。vtableのインデックスやGUIDを間違えても、コンパイラは検出しない。間違いは、実行時のエラーやJVMのクラッシュとして現れる。
- 実行時に`--enable-native-access=ALL-UNNAMED`が必要になる。pom.xmlでは3か所に指定している。テストの`argLine`、`javafx-maven-plugin`の`options`、jpackageの`javaOptions`である。起動の方法を増やすときは、そこにも指定する必要がある。
- 実機の壁紙を変えるテストは、通常のテストから外している（`@Tag("desktop")`）。そのため、`SetWallpaper`の動作の確認は手動の実行に頼る。

### 中立的な結果

- Windowsの「Fit」は画像を常に中央に置く。左寄せや右寄せを実現するには、モニターと同じ大きさの画像をアプリで合成してから設定する必要がある。この合成の仕組みは、別の決定として扱う。
- `IDesktopWallpaper`が使うモニターIDは、モニターのデバイスパスである。設定ファイルは、このデバイスパスをキーにしてモニターごとの設定を保存する。
